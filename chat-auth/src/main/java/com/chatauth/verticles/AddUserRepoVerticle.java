package com.chatauth.verticles;

import com.chatauth.domain.CreateUser;
import com.chatauth.messages.AddUserToDatabase;
import com.chatauth.messages.CreateUserRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;


/**
 * Inserts user in database and responds to the sender with generated id.
 * All done in an async + non-blocking way.
 */
public class AddUserRepoVerticle extends AbstractVerticle {

  private JDBCClient jdbcClient;

  public AddUserRepoVerticle(JDBCClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public void start() {
    final var bus = vertx.eventBus();
    bus.consumer(VerticlePathConstants.ADD_USER_REPO, msg -> {
      final var body = msg.body();
      if (body instanceof AddUserToDatabase req) {
        final var user = req.createUser();
        jdbcClient.getConnection(asyncConnection -> {
          asyncConnection.map(connection ->
            connection.updateWithParams(
              "INSERT INTO \"user\" (username, password) VALUES (?, ?)",
              new JsonArray().add(user.username()).add(user.password()),
              asyncResult -> {
                if (asyncResult.succeeded()) {
                  System.out.println("inserted new user in db");
                  // send back new user id
                  final var newUserId = asyncResult.result().getKeys().getLong(0);
                  req.replyTo().reply(String.valueOf(newUserId));
                } else {
                  req.replyTo().reply("database operation failed");
                }
              }
            )
          );
        });
      }
    });
  }
}
