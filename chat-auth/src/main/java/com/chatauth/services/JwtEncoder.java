package com.chatauth.services;

/**
 * generates JWT token based on username
 */
public interface JwtEncoder {
  String encode(String username);
}
