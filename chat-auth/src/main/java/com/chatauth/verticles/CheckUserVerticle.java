package com.chatauth.verticles;

import com.chatauth.domain.CreateUser;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;

public class CheckUserVerticle extends AbstractVerticle {
  private final JDBCClient client;

  public CheckUserVerticle(JDBCClient client) {
    this.client = client;
  }

  @Override
  public void start() {
    var bus = vertx.eventBus();
    bus.consumer(VerticlePathConstants.CHECK_USER, msg -> {
      // cast to IntialHttpMessage

      System.out.println("check user verticle");
      // pick IntialHttpMessage.createUser
      CreateUser user = (CreateUser) msg.body();
      System.out.println("check user verticle");
      System.out.println(user);

      // class Response(CreateUser createUser, boolean exists);
      // send back to add_user CheckUserResponse(IntialHttpMessage, exists)
      bus.send(VerticlePathConstants.ADD_USER, false);
      // msg.reply(false);

//      client.getConnection(asyncConnection -> {
//        if (asyncConnection.succeeded()) {
//          // var connection = asyncConnection.result();
//          msg.reply(false);


//          connection.queryWithParams(
//            "SELECT COUNT FROM user WHERE username = ?",
//            new JsonArray().add(user.username()), // Use getUsername() instead of username()
//            queryResult -> {
//              if (queryResult.succeeded()) {
//                // if 0 , creat new user
//                if (queryResult.result().getResults().get(0).contains(0)) {
//                  // reply back, to proceed
//                  msg.reply(null);
//                } else {
//                  // reply, user already exists
//                }
//                bus.request(VerticlePathConstants.ADD_USER_REPO, user, asyncReply -> {
//                  if (asyncReply.succeeded()) {
//                    msg.reply(asyncReply.result().body());
//                  } else {
//                    msg.reply(asyncReply.cause().getMessage());
//                  }
//                });
//              } else {
//                msg.reply(queryResult.cause().getMessage());
//              }
//              connection.close(); // Close the database connection
//            }
//          );
//        } else {
//          msg.reply(asyncConnection.cause().getMessage());
//        }
//      });
    });
  }
}
