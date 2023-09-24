package com.chatauth.http;

import com.chatauth.verticles.VerticlePathConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;

public class HttpServerVerticle extends AbstractVerticle {
  @Override
  public void start() {

    CorsHandler corsHandler = CorsHandler.create("*")  // Allow requests from any origin
      .allowedMethod(io.vertx.core.http.HttpMethod.GET)
      .allowedMethod(io.vertx.core.http.HttpMethod.POST)
      .allowedHeader("Content-Type");

    Router router = Router.router(vertx);

    HttpServer server = vertx.createHttpServer();

    // Add the CorsHandler to your routes
    router.route().handler(corsHandler);

    // Create a Router to handle routes
    // Define a route for POST requests to /auth

    router.route(HttpMethod.GET, "/").handler(ctx -> ctx.response().end("hello"));
    router.route(HttpMethod.POST, "/add-user").handler(this::addUser);

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

  /**
   * Thread pool = 8 thread
   * Flow of addUser (asynchronously)
   * 1) HttpServerVerticle - receives all requests and registerd handlers
   * 2) Sends message to AddUserVerticle
   * 3) AddUserVerticle decodes JSON and sends message to AddUserRepoVerticle
   * 4) AddUserRepoVerticl tries to insert data in DB and responds back to AddUserVerticle
   * 5) AddUserVerticle responds back to HttpServerVerticle
   */

  private void addUser(RoutingContext ctx) {
    ctx.request()
      .body()
      .map(Buffer::toJsonObject)
      .onSuccess(userJson -> {
        vertx.eventBus().request(VerticlePathConstants.ADD_USER, userJson, asyncReply -> {
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
