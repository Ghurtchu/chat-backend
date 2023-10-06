package com.chatauth.verticles;

import com.chatauth.messages.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

public class CheckUserVerticle extends AbstractVerticle {
  private final JDBCClient jdbcClient;

  public CheckUserVerticle(JDBCClient client) {
    this.jdbcClient = client;
  }

  @Override
  public void start() {
    var bus = vertx.eventBus();
    bus.consumer(VerticlePathConstants.CHECK_USER, msg -> {
        final var body = msg.body();
        // message from AddUserVerticle
        if (body instanceof CheckUserRequest req) {
          // check if user exists in db
          // if not send request to ADD_USER_REPO,
          // else respond with UserCreationReply(UserCreationStatus.AlreadyExists)
          final var createUser = req.createUserRequest().createUser();
          final var replyTo = req.replyTo();
          final var username = createUser.username();
          jdbcClient.getConnection(asyncConnection -> {
            asyncConnection.map(connection ->
              connection.queryWithParams(
                "SELECT COUNT FROM user WHERE username = ?",
                new JsonArray().add(username),
                asyncQueryResult -> {
                  if (asyncQueryResult.succeeded()) {
                    if (asyncQueryResult.result().getResults().get(0).contains(0)) {
                      // proceed, user does not exist
                      msg.reply(
                        new CheckUserReply(
                          true,
                          "user does not exist",
                          createUser,
                          replyTo
                        )
                      );
                    } else {
                      // user already exists, responds to HttpServerVerticle
                      msg.reply(
                        new CheckUserReply(
                          false,
                          "user already exists",
                          createUser,
                          replyTo
                        )
                      );
                    }
                  } else {
                    // database operation failed
                    msg.reply(
                      new CheckUserReply(
                        false,
                        "database operation failed",
                        createUser,
                        replyTo
                      )
                    );
                  }
                }
              ));
          });
        }
      });
  }
}
