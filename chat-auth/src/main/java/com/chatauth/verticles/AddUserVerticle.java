package com.chatauth.verticles;

import com.chatauth.domain.CreateUser;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

/**
 * Verticle which runs business logic for "Add User" functionality:
 * 1) decodes json from HTTP layer -> CreateUser
 * 2) checks if the username already exists (it requires having a separate Verticle, e.g CheckUserUniquenessVerticle which will reply asynchronously)
 * 3) validates password against business rules (e.g at least 8 chars, at least one uppercase char and so on) and hash it.
 * 4) sends message to AddUserRepoVerticle which inserts user into db and replies with freshly generated user id
 */
public class AddUserVerticle extends AbstractVerticle {

  @Override
  public void start() {
    var bus = vertx.eventBus();
    bus.consumer(VerticlePathConstants.ADD_USER, msg -> {
      // 1) decode jeson
      JsonObject userJson = (JsonObject) msg.body();
      CreateUser createUser = CreateUser.fromJson(userJson);
      System.out.println(createUser);
      /**
       * skipping 2) and 3) parts
       * TODO:
       * - check if user exists in DB
       * - validate and hash password
       */
      // 4) send message to AddUserRepoVerticle through the event bus and register callback
      bus.request(VerticlePathConstants.ADD_USER_REPO, createUser, asyncReply -> {
        if (asyncReply.succeeded()) {
          msg.reply(asyncReply.result().body());
        }
        else {
          msg.reply(asyncReply.cause().getMessage());
        }
      });
    });
  }
}
