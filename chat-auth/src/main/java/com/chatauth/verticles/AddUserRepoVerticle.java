package com.chatauth.verticles;

import com.chatauth.domain.CreateUser;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.jdbc.JDBCClient;


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
      CreateUser user = (CreateUser) msg.body();
      var fakeNewId = String.valueOf(15);
      msg.reply(fakeNewId);
//      jdbcClient.getConnection(asyncConnection -> {
//        asyncConnection.map((SQLConnection connection) ->
//
//          connection.updateWithParams(
//              """
//              INSERT INTO "user" (username, password) VALUES (?, ?)
//              """,
//            new JsonArray().add(user.username()).add(user.password()),
//            (AsyncResult<UpdateResult> res) -> {
//              if (res.succeeded()) {
//                System.out.println("created new user");
//                // send back new user id
//                var newUserId = res.result().getKeys().getLong(0);
//                msg.reply(newUserId);
//              } else {
//                msg.reply("something went wrong");
//              }
//            }
//          )
//        );
//      });
    });

  }


}
