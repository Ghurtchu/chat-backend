package com.chatauth.verticles.serviceverticles;

import com.chatauth.messages.*;
import com.chatauth.messages.login_messages.LoginRequest;
import com.chatauth.messages.login_messages.LoginSuccess;
import com.chatauth.paths.VerticlePathConstants;
import com.chatauth.services.JwtEncoderService;
import io.vertx.core.AbstractVerticle;
import lombok.RequiredArgsConstructor;

/**
 * Verticle which runs business logic for "Add User" functionality:
 * 1) checks if the username already exists (it requires having a separate Verticle, e.g CheckUserUniquenessVerticle which will reply asynchronously)
 * 2) validates password against business rules (e.g at least 8 chars, at least one uppercase char and so on) and hash it.
 * 3) sends message to AddUserRepoVerticle which inserts user into db and replies with freshly generated user id
 */
@RequiredArgsConstructor
public class AuthorizationVerticle extends AbstractVerticle {

  private final JwtEncoderService jwtEncoderService;

  @Override
  public void start() {
    // bus
    var bus = vertx.eventBus();

    // consuming logic
    bus.consumer(VerticlePathConstants.SIGNUP, msg -> {

      // get body
      final var body = msg.body();
      // initial request form HttpServerVerticle
      if (body instanceof CreateUserRequest req) {
        var createUser = req.createUser();
        // send message to CheckUserVerticle
        bus.send(
          VerticlePathConstants.VALIDATE_USER,
          new CheckUserExistenceRequest(createUser)
        );
      }

      else if (body instanceof UserCreated reply) {
        // generate jwt
        // send to http verticle
        final var user = reply.user();
        final String jwt = jwtEncoderService.generateToken(user);
        // send to HTTP - (userId, jwt)
        final var response = new UserJWTGenerated(user.id(), jwt);
        bus.send(
          VerticlePathConstants.HTTP_SIGNUP_REPLY,
          response
        );
      }

      else {
        System.out.println("don't handle");
      }
    });
    // Handles Signup
    bus.consumer(VerticlePathConstants.SIGNUP, msg -> {

      // get body
      final var body = msg.body();
      // initial request form HttpServerVerticle
      if (body instanceof CreateUserRequest req) {
        var createUser = req.createUser();
        // send message to CheckUserVerticle
        bus.send(
          VerticlePathConstants.VALIDATE_USER,
          new CheckUserExistenceRequest(createUser)
        );
      }

      else if (body instanceof UserCreated reply) {
        // generate jwt
        // send to http verticle
        final var user = reply.user();
        final String jwt = jwtEncoderService.generateToken(user);
        // send to HTTP - (userId, jwt)
        final var response = new UserJWTGenerated(user.id(), jwt);
        bus.send(
          VerticlePathConstants.HTTP_SIGNUP_REPLY,
          response
        );
      }

      else {
        System.out.println("don't handle");
      }
    });

    // Handles Login
    bus.consumer(VerticlePathConstants.LOGIN, msg -> {
      final var body = msg.body();
      if (body instanceof LoginRequest req) {
        var createUser = req.createUser();
        bus.send(
          VerticlePathConstants.LOGIN_CHECK,
          new LoginRequest(createUser)
        );

    } else if (body instanceof LoginSuccess reply) {
        // generate jwt
        // send to http verticle
        final var user = reply.user();
        final String jwt = jwtEncoderService.generateToken(user);
        // send to HTTP - (userId, jwt)
        final var response = new UserJWTGenerated(user.id(), jwt);
        bus.send(
          VerticlePathConstants.HTTP_LOGIN_REPLY,
          response
        );
      }
  });
}
}
