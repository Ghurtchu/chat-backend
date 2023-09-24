package com.chatauth;

import com.chatauth.handlers.AuthorizationHandler;
import com.chatauth.verticles.AuthorizationVerticle;
import com.chatauth.verticles.JdbcVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;

public class Main extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    Router router = Router.router(vertx);
    CorsHandler corsHandler = CorsHandler.create("*")  // Allow requests from any origin
      .allowedMethod(io.vertx.core.http.HttpMethod.GET)
      .allowedMethod(io.vertx.core.http.HttpMethod.POST)
      .allowedHeader("Content-Type");

    HttpServer server = vertx.createHttpServer();
    AuthorizationHandler handler = new AuthorizationHandler();
    // Add the CorsHandler to your routes
    router.route().handler(corsHandler);

    // Create a Router to handle routes
    // Define a route for POST requests to /auth
    router.route(HttpMethod.GET, "/").handler(ctx -> ctx.response().end("hello"));
    router.route(HttpMethod.GET, "/auth").handler(handler);
    // Set the router as the request handler for the server
    server.requestHandler(router);

    vertx.deployVerticle(new AuthorizationVerticle());
    vertx.deployVerticle(new JdbcVerticle());
  }
}

