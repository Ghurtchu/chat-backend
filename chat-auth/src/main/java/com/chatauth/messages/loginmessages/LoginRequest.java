package com.chatauth.messages.loginmessages;

import com.chatauth.domain.CreateUser;

public record LoginRequest(CreateUser createUser) {
}
