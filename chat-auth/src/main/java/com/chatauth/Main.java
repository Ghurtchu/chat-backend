package com.chatauth;

import com.chatauth.codecs.CreateUserMessageCodec;
import com.chatauth.domain.CreateUser;
import com.chatauth.verticles.AddUserVerticle;
import com.chatauth.http.HttpServerVerticle;
import com.chatauth.verticles.AddUserRepoVerticle;
import com.chatauth.verticles.CheckUserVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;


public class Main extends AbstractVerticle {


  public static void main(String[] args) {

    // get global vertx instance
    final var vertx = Vertx.vertx();

    

    // deployment options
    final var options = new DeploymentOptions().setInstances(16);

    // create configuration on the fly
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
    vertx.eventBus().registerDefaultCodec(CreateUser.class, new CreateUserMessageCodec());

    // deploy verticles so that they are ready to receive and send messages to each other
    vertx.deployVerticle(new HttpServerVerticle(), options);
    vertx.deployVerticle(new AddUserRepoVerticle(jdbcClient), options);
    vertx.deployVerticle(new AddUserVerticle(), options);
    vertx.deployVerticle(new CheckUserVerticle(jdbcClient), options);
  }
}

