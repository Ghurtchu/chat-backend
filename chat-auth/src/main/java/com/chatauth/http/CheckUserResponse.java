package com.chatauth.http;

import com.chatauth.messages.InitialHttpMessage;

public record CheckUserResponse (InitialHttpMessage intialHttpMessage, boolean exists) { }
