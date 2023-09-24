package com.chatauth.services;

import com.chatauth.domain.User;
import io.jsonwebtoken.Claims;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * generates JWT token based on username
 */
public interface JwtEncoder {
  String extractUsername(String token);

  Claims extractAllClaims(String token);

  <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

  Key getSignInKey();

  String generateToken(Map<String, Object> extraClaims,
                       User user);

  String generateToken(User user);

  boolean isTokenValid(String token, User user);

  boolean isTokenExpired(String token);

  Date extractExpiration(String token);
}
