package com.chatauth.verticles;

import com.chatauth.handlers.AuthorizationHandler;
import com.chatauth.model.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;


public class JdbcVerticle extends AbstractVerticle {
  private JDBCClient jdbcClient;
  @Override
  public void start() throws Exception {
    JsonObject config = new JsonObject()
      .put("url", "jdbc:postgresql://localhost:5432/mydatabase")
      .put("driver_class", "org.postgresql.Driver")
      .put("user", "postgres")
      .put("password", "123");

    jdbcClient = JDBCClient.createShared(vertx, config);

    // here
    CorsHandler corsHandler = CorsHandler.create("*")  // Allow requests from any origin
      .allowedMethod(io.vertx.core.http.HttpMethod.GET)
      .allowedMethod(io.vertx.core.http.HttpMethod.POST)
      .allowedHeader("Content-Type");

    Router router = Router.router(vertx);

    HttpServer server = vertx.createHttpServer();
    AuthorizationHandler handler = new AuthorizationHandler();
    // Add the CorsHandler to your routes
    router.route().handler(corsHandler);

    // Create a Router to handle routes
    // Define a route for POST requests to /auth
    router.route(HttpMethod.GET, "/").handler(ctx -> ctx.response().end("hello"));
    router.route(HttpMethod.GET, "/auth").handler(handler);
    router.route(HttpMethod.POST, "/add-user").handler(this::addUser);
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

    private void addUser(RoutingContext ctx) {
     var request = ctx.request();
     var futureUser = request.body()
              .map(Buffer::toJsonObject)
              .map(CreatUser::fromJson);
     jdbcClient.getConnection((AsyncResult<SQLConnection> asyncConenction) -> {
        futureUser.compose((CreatUser user) -> {
          var res = asyncConenction.map((SQLConnection conn) -> conn.updateWithParams(
                      """
                      INSERT INTO "user" (username, password) VALUES (?, ?)
                      """,
                  new JsonArray().add(user.username()).add(user.password()),
                  (AsyncResult<UpdateResult> updateResult) -> {
                    // side effect
                    if (updateResult.succeeded()) {
                      var result = updateResult.result();
                      long id = updateResult.result().getKeys().getLong(0);
                      request.response().end("gaiparsa");
                    } else {
                      request.response().end("gaedo");
                    }
                  }));
          return res.succeeded() ? Future.succeededFuture() : Future.failedFuture(res.cause());
        });
      });
  }
}
