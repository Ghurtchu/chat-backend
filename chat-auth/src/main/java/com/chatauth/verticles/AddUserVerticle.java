package com.chatauth.verticles;

import com.chatauth.messages.AddUserToDatabase;
import com.chatauth.messages.CheckUserReply;
import com.chatauth.messages.CreateUserRequest;
import com.chatauth.messages.CheckUserRequest;
import io.vertx.core.AbstractVerticle;

/**
 * Verticle which runs business logic for "Add User" functionality:
 * 1) checks if the username already exists (it requires having a separate Verticle, e.g CheckUserUniquenessVerticle which will reply asynchronously)
 * 2) validates password against business rules (e.g at least 8 chars, at least one uppercase char and so on) and hash it.
 * 3) sends message to AddUserRepoVerticle which inserts user into db and replies with freshly generated user id
 */
public class AddUserVerticle extends AbstractVerticle {

  @Override
  public void start() {
    var bus = vertx.eventBus();
                                                // replyTo from Http layer
    bus.consumer(VerticlePathConstants.ADD_USER, msg -> {
      final var body = msg.body();
      // initial message - CreateUserRequest, from HttpServerVerticle
      // save msg somehow so that we can respond it later from different verticle

      if (body instanceof CreateUserRequest req) {
        var createUser = req.createUser();
        // send message to CheckUserVerticle
        bus.send(
          VerticlePathConstants.CHECK_USER,
          new CheckUserRequest(req, msg)
        );
      }

      else if (body instanceof CheckUserReply rep) {
        final var proceed = rep.proceed();
        final var replyTo = rep.replyTo();
        if (proceed) {
          bus.send(
            VerticlePathConstants.ADD_USER_REPO,
            new AddUserToDatabase(rep.createUser(), replyTo)
          );
        } else {
          replyTo.reply(rep.reason());
        }
      }

      else {

      }
    });
  }
}
