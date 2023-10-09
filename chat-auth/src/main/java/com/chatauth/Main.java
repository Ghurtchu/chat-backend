package com.chatauth;

import com.chatauth.codecs.*;
import com.chatauth.domain.CreateUser;
import com.chatauth.messages.*;
import com.chatauth.services.implementation.JwtEncoderImpl;
import com.chatauth.verticles.SignupVerticle;
import com.chatauth.http.HttpServerVerticle;
import com.chatauth.verticles.AddUserRepoVerticle;
import com.chatauth.verticles.UserValidatorVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import org.hibernate.annotations.Check;


public class Main extends AbstractVerticle {

  // Verticle (A, B, C, D)
  // A -> B, B -> C, C -> D, problem: D -> A -> how to respond from D to A

  public static void main(String[] args) {

    // get global vertx instance
    // final var = const (js), val (scala)
    // final var = can't reassign to variable
    final var vertx = Vertx.vertx();
    // TODO: later read from configuration
    final var config = new JsonObject()
      .put("url", "jdbc:postgresql://localhost:5432/mydatabase")
      .put("driver_class", "org.postgresql.Driver")
      .put("user", "postgres")
      .put("password", "123");

    // jdbc client
    final var jdbcClient = JDBCClient.createShared(vertx, config);

    // register codecs for sending and receiving messages between verticles
    // codecs are necessary for se/deserializing verticle messages

    registerCodecs(vertx);

    // deploy verticles so that they are ready to receive and send messages to each other
    vertx.deployVerticle(new HttpServerVerticle());
    vertx.deployVerticle(new AddUserRepoVerticle(jdbcClient));
    vertx.deployVerticle(new SignupVerticle(new JwtEncoderImpl()));
    vertx.deployVerticle(new UserValidatorVerticle(jdbcClient));
  }

  private static void registerCodecs(Vertx vertx) {
    vertx.eventBus().registerDefaultCodec(CreateUser.class, new CreateUserCodec());
    vertx.eventBus().registerDefaultCodec(AddUserToDatabase.class,
                                        new AddUserToDatabaseCodec());
    vertx.eventBus().registerDefaultCodec(CheckUserExistenceRequest.class,
                                        new CheckUserExistenceRequestCodec());
    vertx.eventBus().registerDefaultCodec(CreateUserRequest.class,
                                          new CreateUserRequestCodec());
    vertx.eventBus().registerDefaultCodec(UserAlreadyExists.class,
                                          new UserAlreadyExistsCodec());
    vertx.eventBus().registerDefaultCodec(UserCreated.class,
                                          new UserCreatedCodec());
    vertx.eventBus().registerDefaultCodec(UserJWTGenerated.class,
                                          new UserJWTGeneratedCodec());
    vertx.eventBus().registerDefaultCodec(PasswordCheckFailedMessage.class,
                                          new PasswordCheckFailedMessageCodec());
  }

}

