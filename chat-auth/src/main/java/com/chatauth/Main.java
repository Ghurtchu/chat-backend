package com.chatauth;

import com.chatauth.handlers.AuthorizationHandler;
import com.chatauth.verticles.AuthorizationVerticle;
import com.chatauth.verticles.JdbcVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;


public class Main extends AbstractVerticle {


  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new AuthorizationVerticle());
    vertx.deployVerticle(new JdbcVerticle());
  }
}

