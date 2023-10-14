package com.chatauth.messages.login_messages;

import com.chatauth.domain.CreateUser;

public record LoginRequest(CreateUser createUser) {
}
