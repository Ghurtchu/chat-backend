package com.chatauth.verticles;

import com.chatauth.handlers.AuthorizationHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

public class AuthorizationVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {
    HttpServer server = vertx.createHttpServer();
    AuthorizationHandler handler = new AuthorizationHandler();
    // Create a Router to handle routes
    Router router = Router.router(vertx);
    // Define a route for POST requests to /auth
    router.route(HttpMethod.GET, "/").handler(ctx -> ctx.response().end("hello"));
    router.route(HttpMethod.GET, "/auth").handler(handler);
    // Set the router as the request handler for the server
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
}
