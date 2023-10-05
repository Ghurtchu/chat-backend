package com.chatauth.verticles;

import com.chatauth.messages.CreateUserRequest;
import com.chatauth.messages.UserCreationReply;
import io.vertx.core.AbstractVerticle;

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
                                                // msg from Http layer
    bus.consumer(VerticlePathConstants.ADD_USER, msg -> {
      var body = msg.body();
      // initial message from HttpServerVerticle
      if (body instanceof CreateUserRequest request) {
        var createUser = request.createUser();
        System.out.println(createUser);
        // send message to AddUserVerticle
        bus.send(VerticlePathConstants.CHECK_USER, new CreateUserRequest(createUser));
      } else if (body instanceof UserCreationReply reply) {
        // idk yet
        var exists = (Boolean) body;
        if (exists) {
          // Http utxari ro arsebobs
          // resp.httpVerticleMessage.reply("User already exists, change your username");

        } else {
          // chasvi bazashi
          bus.request(VerticlePathConstants.ADD_USER_REPO, null);
        }
      }

    });
  }
}
