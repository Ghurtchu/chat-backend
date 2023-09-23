package com.chatauth.model;

import io.vertx.core.json.JsonObject;

/**
 * used for processing HTTP request body
 * it does not have id, only groups username and password
 */
public record CreatUser(String username, String password) {

    public static CreatUser fromJson(JsonObject js) {
        return new CreatUser(
                js.getString("username"),
                js.getString("password")
        );
    }
}
