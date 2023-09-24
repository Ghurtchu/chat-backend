package com.chatauth.messages;

import com.chatauth.domain.CreateUser;
import io.vertx.core.eventbus.Message;

public record InitialHttpMessage (Message<Object> httpVerticleMessage, CreateUser createUser) { }
