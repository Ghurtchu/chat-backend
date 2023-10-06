package com.chatauth.messages;

import com.chatauth.domain.CreateUser;
import io.vertx.core.eventbus.Message;

public record CheckUserReply(boolean proceed, String reason, CreateUser createUser, Message<Object> replyTo) {
}
