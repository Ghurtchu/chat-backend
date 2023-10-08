package com.chatauth.verticles;

import com.chatauth.messages.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;

public class UserValidatorVerticle extends AbstractVerticle {
  private final JDBCClient jdbcClient;

  public UserValidatorVerticle(JDBCClient client) {
    this.jdbcClient = client;
  }

  @Override
  public void start() {
    var bus = vertx.eventBus();
    bus.consumer(VerticlePathConstants.VALIDATE_USER, msg -> {
        final var body = msg.body();
        // message from SignupVerticle
        if (body instanceof CheckUserExistenceRequest req) {
          // check if user exists in db
          // if not send request to ADD_USER_REPO,
          // else ???
          final var createUser = req.createUser();
          final var username = createUser.username();
          jdbcClient.getConnection(asyncConnection -> {
            asyncConnection.map(connection ->
              connection.queryWithParams(
                "SELECT COUNT FROM \"user\" WHERE username = ?",
                new JsonArray().add(username),
                asyncQueryResult -> {
                  // if query was successful
                  if (asyncQueryResult.succeeded()) {
                    // if 0 was returned = username does not exist
                    if (asyncQueryResult.result().getResults().get(0).contains(0)) {
                      // proceed, user does not exist
                      // bus.send()
                      bus.send(
                        VerticlePathConstants.ADD_USER_REPO,
                        new AddUserToDatabase(createUser)
                      );
                    } else {
                      // user already exists, responds to HttpServerVerticle
                      bus.send(
                        VerticlePathConstants.HTTP_REPLY,
                        UserAlreadyExists.getInstance()
                      );
                    }
                  } else {
                    final var failureReason = asyncQueryResult.cause();
                    // database operation failed
                    // TODO: create singleton object for DB operation failure
                    // public record DbOpsFailed(String message) { }
                    bus.send(
                      VerticlePathConstants.HTTP_REPLY,
                      "db operation failed"
                    );
                  }
                }
              ));
          });
        }
      });
  }
}
