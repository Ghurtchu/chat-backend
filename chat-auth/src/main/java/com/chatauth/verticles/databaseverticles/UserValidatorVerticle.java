package com.chatauth.verticles.databaseverticles;

import com.chatauth.messages.*;
import com.chatauth.paths.VerticlePathConstants;
import com.chatauth.services.implementation.ValidatePasswordServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserValidatorVerticle extends AbstractVerticle {
  private final JDBCClient jdbcClient;

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
          final var password = createUser.password();
          final String passwordCheck = ValidatePasswordServiceImpl.checkPassword(password);
          if (passwordCheck.equals("valid")) {
            jdbcClient.getConnection(asyncConnection -> {
              asyncConnection.map(connection ->
                connection.queryWithParams(
                  "SELECT COUNT(*) FROM \"user\" WHERE username = ?",
                  new JsonArray().add(username),
                  asyncQueryResult -> {
                    // if query was successful
                    if (asyncQueryResult.succeeded()) {
                      // if 0 was returned = username does not exist
                      System.out.println(asyncQueryResult.result().getResults().get(0));
                      var result = asyncQueryResult.result().getResults().get(0);
                      var expected = new JsonArray().add(0, 0L);
                      if (result.equals(expected)) {
                        // proceed, user does not exist
                        // bus.send()
                        bus.send(
                          VerticlePathConstants.ADD_USER_REPO,
                          new AddUserToDatabase(createUser)
                        );
                      } else {
                        // user already exists, responds to HttpServerVerticle
                        bus.send(
                          VerticlePathConstants.HTTP_SIGNUP_REPLY,
                          UserAlreadyExists.getInstance()
                        );
                      }
                    } else {
                      final var failureReason = asyncQueryResult.cause();
                      // database operation failed
                      // TODO: create singleton object for DB operation failure
                      // public record DbOpsFailed(String message) { }
                      bus.send(
                        VerticlePathConstants.HTTP_SIGNUP_REPLY,
                        "db operation failed"
                      );
                    }
                  }
                ));
            });
          }
          else
            bus.send(VerticlePathConstants.HTTP_SIGNUP_REPLY, new PasswordCheckFailedMessage(passwordCheck));
        }
      });

  }
}
