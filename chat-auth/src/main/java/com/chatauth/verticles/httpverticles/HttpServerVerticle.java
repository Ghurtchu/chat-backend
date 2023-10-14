package com.chatauth.verticles.httpverticles;

import com.chatauth.domain.CreateUser;
import com.chatauth.messages.CreateUserRequest;
import com.chatauth.messages.PasswordCheckFailedMessage;
import com.chatauth.messages.login_messages.IncorrectPasswordMessage;
import com.chatauth.messages.login_messages.LoginRequest;
import com.chatauth.messages.UserJWTGenerated;
import com.chatauth.paths.VerticlePathConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.RequiredArgsConstructor;

/**
 * Creates http server instance.
 * Accepts requests on predefined paths and sends messages to different verticles.
 *
 */
@RequiredArgsConstructor
public class HttpServerVerticle extends AbstractVerticle {

  final int port;
  final String host;

  @Override
  public void start() {

    // Allow requests from any origin
    final var corsHandler = CorsHandler.create("*")
      .allowedMethod(io.vertx.core.http.HttpMethod.GET)
      .allowedMethod(io.vertx.core.http.HttpMethod.POST)
      .allowedHeader("Content-Type");

    final var router = Router.router(vertx);
    router.route().handler(corsHandler);

    final var server = vertx.createHttpServer();

    defineRoutes(router);

    // set handler to server
    server.requestHandler(router);

    server.listen(port, host, asyncResult -> {
      if (asyncResult.succeeded()) {
        System.out.printf("Server is running on port %s%n", port);
      } else {
        System.err.println("Failed to start server: " + asyncResult.cause());
      }
    });
  }

  private void defineRoutes(Router router) {
    router.route(HttpMethod.GET, "/").handler(ctx -> ctx.response().end("hello"));
    router.route(HttpMethod.POST, "/signup").handler(this::signup);
    router.route(HttpMethod.POST, "/login").handler(this::login);
  }

  private void signup(RoutingContext ctx) {
    ctx.request() // req
      .body() // body
      .map(Buffer::toJsonObject) // parse json
      .onSuccess(userJson -> { // if success
        // create special msg for ADD_USER
        final var msg = new CreateUserRequest(CreateUser.fromJson(userJson));
        // event bus
        final var bus = vertx.eventBus();
        // request logic
        bus.send(VerticlePathConstants.SIGNUP, msg);
        // consuming logic
        bus.consumer(
          VerticlePathConstants.HTTP_SIGNUP_REPLY,
          asyncReply -> {
            final var body = asyncReply.body();
            if (body instanceof UserJWTGenerated reply) {
              var js = new JsonObject()
                .put("userId", Long.toString(reply.userId()))
                .put("jwt", reply.jwt());
              ctx.request().response().end(js.encodePrettily());
            }
            else if (body instanceof PasswordCheckFailedMessage reply) {
              ctx.request().response().end("cause: " + reply.reasonForFailure());
            }
            else {
              System.out.println("not handled");
              System.out.println(body);
            }
          }
        );
      })
      .onFailure(err -> ctx.request().response().end("Incorrect JSON format"));
  }

  public void login(RoutingContext ctx) {
    ctx.request() // req
      .body() // body
      .map(Buffer::toJsonObject) // parse json
      .onSuccess(userJson -> {
        final var msg = new LoginRequest(CreateUser.fromJson(userJson));
        // event bus
        final var bus = vertx.eventBus();
        bus.send(VerticlePathConstants.LOGIN, msg);

        bus.consumer(
          VerticlePathConstants.HTTP_LOGIN_REPLY,
          asyncReply -> {
            final var body = asyncReply.body();
            if (body instanceof IncorrectPasswordMessage reply) {
              ctx.request().response().end("Incorrect Password");
            }

            else if (body instanceof UserJWTGenerated reply) {
              var js = new JsonObject()
                .put("userId", Long.toString(reply.userId()))
                .put("jwt", reply.jwt());
              ctx.request().response().end(js.encodePrettily());
            } else {
              System.out.println("not handled");
              System.out.println(body);
            }
          });
      });
  }
}
