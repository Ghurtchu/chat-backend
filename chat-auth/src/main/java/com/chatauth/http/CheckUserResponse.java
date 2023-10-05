package com.chatauth.http;

import com.chatauth.messages.CreateUserRequest;

public record CheckUserResponse (CreateUserRequest intialHttpMessage, boolean exists) { }
