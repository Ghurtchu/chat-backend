package com.chatauth;

import com.chatauth.codecs.CreateUserMessageCodec;
import com.chatauth.domain.CreateUser;
import com.chatauth.verticles.AddUserVerticle;
import com.chatauth.http.HttpServerVerticle;
import com.chatauth.verticles.AddUserRepoVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;


public class Main extends AbstractVerticle {


  public static void main(String[] args) {

    var vertx = Vertx.vertx();

    // TODO: later read from configuration
    var config = new JsonObject()
      .put("url", "jdbc:postgresql://localhost:5432/mydatabase")
      .put("driver_class", "org.postgresql.Driver")
      .put("user", "postgres")
      .put("password", "123");

    // jdbc client
    var jdbcClient = JDBCClient.createShared(vertx, config);

    // register codecs for sending and receiving messages between verticles
    vertx.eventBus().registerDefaultCodec(CreateUser.class, new CreateUserMessageCodec());

    // deploy verticles
    vertx.deployVerticle(new HttpServerVerticle());
    vertx.deployVerticle(new AddUserRepoVerticle(jdbcClient));
    vertx.deployVerticle(new AddUserVerticle());
  }
}

