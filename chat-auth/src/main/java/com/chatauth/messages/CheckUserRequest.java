package com.chatauth.messages;

import io.vertx.core.eventbus.Message;

public record CheckUserRequest(CreateUserRequest createUserRequest, Message<Object> replyTo) {
}
