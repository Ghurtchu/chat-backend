package com.chatauth.messages;

import com.chatauth.domain.CreateUser;

public record LoginRequest(CreateUser createUser) {
}
