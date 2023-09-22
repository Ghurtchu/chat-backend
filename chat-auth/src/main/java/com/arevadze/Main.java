package com.arevadze;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class Main extends AbstractVerticle {


  public static void main(String[] args) {

    // Create a Vert.x instance
    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new Main()); // Example AuthVerticle
  }

  @Override
  public void start() throws Exception {
    HttpServer server = vertx.createHttpServer();

    // Create a Router to handle routes
    Router router = Router.router(vertx);

    // Define a route for POST requests to /auth
    router.route(HttpMethod.GET, "/").handler(ctx -> ctx.response().end("hello"));
    router.route(HttpMethod.POST, "/auth").handler(this::handleAuth);

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

  private void handleAuth(RoutingContext ctx) {
    var json = ctx.body(); //vitom json
    var response = ctx.response();

    response.end(JsonObject.of("jwt", "fake jwt response").encode());

  }


}

