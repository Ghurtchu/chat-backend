package com.chatauth.verticles.httpverticles;

import com.chatauth.domain.CreateUser;
import com.chatauth.messages.CreateUserRequest;
import com.chatauth.messages.PasswordCheckFailedMessage;
import com.chatauth.messages.login_messages.IncorrectPasswordMessage;
import com.chatauth.messages.login_messages.LoginRequest;
import com.chatauth.messages.UserJWTGenerated;
import com.chatauth.paths.VerticlePathConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

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

  /**
   * handles signup functionality in non-blocking and async I/O
   */
  private void signup(RoutingContext ctx) {
    ctx.request()
      .body()
      .map(Buffer::toJsonObject)
      .onSuccess(userJson -> {
        // create special msg for AuthorizationVerticle
        final var msg = new CreateUserRequest(CreateUser.fromJson(userJson));
        final var bus = vertx.eventBus();
        // initial signup attempt: send message to AuthorizationVeritcle
        bus.send(VerticlePathConstants.SIGNUP, msg);
        // message consuming logic
        bus.consumer(
          VerticlePathConstants.HTTP_SIGNUP_REPLY,
          asyncReply -> {
            final var body = asyncReply.body();
            // JWT was generated
            if (body instanceof UserJWTGenerated reply) {
              final var jsonResponse = new JsonObject()
                .put("userId", Long.toString(reply.userId()))
                .put("jwt", reply.jwt());
              ctx.request()
                .response()
                .end(jsonResponse.encodePrettily());
            }
            // signup failed, password was not strong enough
            else if (body instanceof PasswordCheckFailedMessage reply) {
              final var jsonResponse = new JsonObject()
                .put("failureReason", reply.reason());
              ctx.request()
                .response()
                .end(jsonResponse.encodePrettily());
            }
            else {
              System.out.println("Not handled");
              System.out.println(body);
            }
          }
        );
      })
      .onFailure(invalidJson(ctx));
  }

  /**
   * handles login functionality in non-blocking and async I/O
   */
  public void login(RoutingContext ctx) {
    ctx.request()
      .body()
      .map(Buffer::toJsonObject)
      .onSuccess(userJson -> {
        final var msg = new LoginRequest(CreateUser.fromJson(userJson));
        final var bus = vertx.eventBus();
        // send initial message to login verticle
        bus.send(VerticlePathConstants.LOGIN, msg);
        // message consuming logic
        bus.consumer(
          VerticlePathConstants.HTTP_LOGIN_REPLY,
          asyncReply -> {
            final var body = asyncReply.body();
            // password was incorrect
            if (body instanceof IncorrectPasswordMessage) {
              final var jsonResponse = new JsonObject()
                .put("failureReason", "incorrect username or password");
              ctx.request()
                .response()
                .end(jsonResponse.encodePrettily());
            }
            // JWT was generated
            else if (body instanceof UserJWTGenerated reply) {
              final var jsonResponse = new JsonObject()
                .put("userId", Long.toString(reply.userId()))
                .put("jwt", reply.jwt());
              ctx.request()
                .response()
                .end(jsonResponse.encodePrettily());
            } else {
              System.out.println("not handled");
              System.out.println(body);
            }
          });
      })
      .onFailure(invalidJson(ctx));
  }

  @NotNull
  private static Handler<Throwable> invalidJson(RoutingContext ctx) {
    return err -> ctx.request().response().end("Invalid JSON format");
  }
}
