package com.chatauth.http;

import com.chatauth.domain.CreateUser;
import com.chatauth.messages.CreateUserRequest;
import com.chatauth.verticles.VerticlePathConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;

/**
 * Creates http server instance.
 * Accepts requests on predefined paths and sends messages to different verticles.
 * For now it accepts request on /add-user and sends CreateUserRequest(...) message to AddUserVerticle
 * ...
 * ...
 * Finally it receives response from some verticle and then sends the response to the client.
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
    router.route(HttpMethod.POST, "/add-user").handler(this::addUser);

    // set handler to server
    server.requestHandler(router);

    // Listen on port 8080
    server.listen(8080, result -> {
      if (result.succeeded()) {
        System.out.println("Server is running on port 8080");
      } else {
        System.err.println("Failed to start server: " + result.cause());
      }
    });
  }
  private void addUser(RoutingContext ctx) {
    ctx.request()
      .body()
      .map(Buffer::toJsonObject)
      .onSuccess(userJson -> {
        // send message to AddUserVerticle
        final var msg = new CreateUserRequest(CreateUser.fromJson(userJson));
        vertx.eventBus().request(VerticlePathConstants.ADD_USER, msg, asyncReply -> {
          if (asyncReply.succeeded()) {
            ctx.request().response().end(asyncReply.result().body().toString());
          } else {
            ctx.request().response().end("Something went wrong");
          }
        });
      })
      .onFailure(err -> ctx.request().response().end("Error during json decoding"));
  }



}
