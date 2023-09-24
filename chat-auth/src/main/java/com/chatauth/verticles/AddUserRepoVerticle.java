package com.chatauth.verticles;

import com.chatauth.domain.CreateUser;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;


/**
 * Verticle which inserts user in database and responds to the sender with generated id
 * in an async + non-blocking way.
 */
public class AddUserRepoVerticle extends AbstractVerticle {

  private JDBCClient jdbcClient;

  public AddUserRepoVerticle(JDBCClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  @Override
  public void start() {
    var bus = vertx.eventBus();
    bus.consumer(VerticlePathConstants.ADD_USER_REPO, msg -> {
      // tu unda chawero -> id
      // tu ar unda chawero -> false

      CreateUser user = (CreateUser) msg.body();
      jdbcClient.getConnection(asyncConnection -> {
        asyncConnection.map(connection ->
          connection.updateWithParams(
            "INSERT INTO \"user\" (username, password) VALUES (?, ?)",
            new JsonArray().add(user.username()).add(user.password()),
            asyncResult -> {
              if (asyncResult.succeeded()) {
                System.out.println("inserted new user in db");
                // send back new user id
                var newUserId = asyncResult.result().getKeys().getLong(0);
                msg.reply(String.valueOf(newUserId));
              } else {
                msg.reply("DB operation failed");
              }
            }
          )
        );
      });
    });

  }


}
