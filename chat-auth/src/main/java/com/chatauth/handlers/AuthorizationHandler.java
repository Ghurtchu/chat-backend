package com.chatauth.handlers;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class AuthorizationHandler implements Handler<RoutingContext> {

  @Override
  public  void handle(RoutingContext ctx) {
    var json = ctx.body(); //vitom json
    var response = ctx.response();

    response.end(JsonObject.of("jwt", "fake jwt response").encode());
  }
}
