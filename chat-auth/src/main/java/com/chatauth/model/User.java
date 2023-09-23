package com.chatauth.model;

// gadaxede java recordebs
/**
 * used for inserting new users into db
 * returns NewUser for which id represents auto generated primary key value
 */
public record User(Long id, String username, String password) { }
