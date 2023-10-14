package com.chatauth.domain;

// records
// pattern matching
// sealed interfaces (data modeling)

import lombok.Builder;

/**
 * used for inserting new users into db
 * returns NewUser for which id represents auto generated primary key value
 */
@Builder
public record User(Long id, String username, String password) { }
