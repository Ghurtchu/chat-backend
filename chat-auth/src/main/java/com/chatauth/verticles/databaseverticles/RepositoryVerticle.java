package com.chatauth.verticles.databaseverticles;

import com.chatauth.domain.User;
import com.chatauth.messages.AddUserToDatabase;
import com.chatauth.messages.UserCreated;
import com.chatauth.messages.login_messages.LoginRequest;
import com.chatauth.messages.login_messages.LoginSuccess;
import com.chatauth.paths.VerticlePathConstants;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.RequiredArgsConstructor;


/**
 * Inserts user in database and responds to the sender with generated id.
 * All done in an async + non-blocking way.
 */
@RequiredArgsConstructor
public class RepositoryVerticle extends AbstractVerticle {

  private final JDBCClient jdbcClient;
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
                    VerticlePathConstants.HTTP_SIGNUP_REPLY,
                    "something went wrong"
                  );
                }
              }
            )
          );
        });
      }
    });

    bus.consumer(VerticlePathConstants.LOGIN_CHECK, msg -> {

      final var body = msg.body();

      if (body instanceof LoginRequest req) {

        jdbcClient.getConnection(asyncConnection -> {

          final var user = req.createUser();
          asyncConnection.map(connection -> connection.queryWithParams(
            "SELECT * FROM \"user\" WHERE username = ? AND password = ?",
            new JsonArray().add(user.username()).add(user.password()),
            asyncResult -> {

              if (asyncResult.succeeded()) {
                // send back new user id
                System.out.println(asyncResult.result().getResults().get(0).getLong(0).toString());
                long userId = asyncResult.result().getResults().get(0).getLong(0);
                  User user1 =
                    User.builder()
                    .id(userId)
                    .username(user.username())
                    .password(user.password()).build();
                bus.send(
                  VerticlePathConstants.LOGIN,
                  new LoginSuccess(user1)
                );

              } else {
                bus.send(
                  VerticlePathConstants.HTTP_LOGIN_REPLY,
                  "something went wrong"
                );
              }
              })); });
            };
        });
      }
    }


