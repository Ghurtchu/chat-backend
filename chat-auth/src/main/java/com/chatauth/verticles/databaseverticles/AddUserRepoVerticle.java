package com.chatauth.verticles.databaseverticles;

import com.chatauth.domain.User;
import com.chatauth.messages.AddUserToDatabase;
import com.chatauth.messages.UserCreated;
import com.chatauth.paths.VerticlePathConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;


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
    // bus
    final var bus = vertx.eventBus();
    // consuming logic
    bus.consumer(VerticlePathConstants.ADD_USER_REPO, msg -> {
      // body
      final var body = msg.body();
      // msg from UserValidatorVerticle
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
                  final var newUser = new User(newUserId, user.username(), user.password());
                  final var response = new UserCreated(newUser);
                  bus.send(
                    VerticlePathConstants.SIGNUP,
                    response
                  );
                } else {
                  bus.send(
                    VerticlePathConstants.HTTP_REPLY,
                    "something went wrong"
                  );
                }
              }
            )
          );
        });
      }
    });
  }
}
