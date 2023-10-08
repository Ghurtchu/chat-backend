package com.chatauth.messages;

import com.chatauth.domain.CreateUser;

/**
 * Verticle message for checking user existence in DB
 */
public record CheckUserExistenceRequest(CreateUser createUser) { }
