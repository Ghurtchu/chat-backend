package com.chatauth.http;

import com.chatauth.domain.CreateUser;
import com.chatauth.messages.CreateUserRequest;
import com.chatauth.messages.UserJWTGenerated;
import com.chatauth.verticles.VerticlePathConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.function.Function;

/**
 * Creates http server instance.
 * Accepts requests on predefined paths and sends messages to different verticles.
 * For now it accepts request on /signup and sends CreateUserRequest(...) message to AddUserVerticle
 * ...
 * ...
 * Finally it receives response from some verticle and then sends the response (jwt token) to the client.
 */
public class HttpServerVerticle extends AbstractVerticle {

  @Override
  public void start() {

    // Allow requests from any origin
    final var corsHandler = CorsHandler.create("*")
      .allowedMethod(io.vertx.core.http.HttpMethod.GET)
      .allowedMethod(io.vertx.core.http.HttpMethod.POST)
      .allowedHeader("Content-Type");

    // Create router
    final var router = Router.router(vertx);

    // Create server
    final var server = vertx.createHttpServer();

    // Add the CorsHandler to your routes
    router.route().handler(corsHandler);

    // Define a route for health check
    router.route(HttpMethod.GET, "/").handler(ctx -> ctx.response().end("hello"));
    // Define a route for POST requests to /add-user
    router.route(HttpMethod.POST, "/signup").handler(this::signup);

    router.route(HttpMethod.POST, "/login").handler(null);

    // set handler to server
    server.requestHandler(router);

    // Listen on port 8080
    server.listen(8081, result -> {
      if (result.succeeded()) {
        System.out.println("Server is running on port 8080");
      } else {
        System.err.println("Failed to start server: " + result.cause());
      }
    });
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
          VerticlePathConstants.HTTP_REPLY,
          asyncReply -> {
            final var body = asyncReply.body();
            if (body instanceof UserJWTGenerated reply) {
              var js = new JsonObject()
                .put("userId", Long.toString(reply.userId()))
                .put("jwt", reply.jwt());
              ctx.request().response().end(js.encodePrettily());
            } else {
              System.out.println("not handled");
              System.out.println(body);
            }
          }
        );
      })
      .onFailure(err -> ctx.request().response().end("Incorrect JSON format"));
  }



}
