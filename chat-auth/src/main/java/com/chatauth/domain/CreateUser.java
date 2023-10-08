package com.chatauth.domain;

import io.vertx.core.json.JsonObject;

/**
 * Command from UI
 * used for processing HTTP request body
 * it does not have id, only groups username and password
 */
public record CreateUser(String username, String password) {

    public static CreateUser fromJson(JsonObject js) {
        return new CreateUser(
                js.getString("username"),
                js.getString("password")
        );
    }
}
