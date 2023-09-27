package com.chatauth.services.implementation;

import com.chatauth.domain.User;
import com.chatauth.services.JwtEncoder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class JwtEncoderImpl implements JwtEncoder {
  private final Logger logger = LoggerFactory.getLogger("logger");
  private static final String SECRET_KEY =
    "2646294A404E635266546A576E5A7234753778214125442A472D4B6150645367";
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
      .setSigningKey(getSignInKey())
      .build()
      .parseClaimsJws(token)
      .getBody();
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  public Key getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);

    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateToken(Map<String, Object> extraClaims,
                              User user) {
    long issuedAt = System.currentTimeMillis();
    long expiresAtUser = issuedAt + 1000 * 60 * 60 * 24 * 5;
    long expiresAt = expiresAtUser;
    Date issued = new Date(issuedAt);
    logger.info("token created for " + user.username());
    Date expires = new Date(expiresAt);
    logger.info("token created at: " + issued);
    logger.info("token will expire at" + expires);
    return Jwts
      .builder().setClaims(extraClaims)
      .setSubject(user.username())
      .setIssuedAt(issued)
      .setExpiration(expires)
      .signWith(getSignInKey(), SignatureAlgorithm.HS256)
      .compact();
  }


  public String generateToken(User user) {
    return generateToken(new HashMap<>(), user);
  }

  public boolean isTokenValid(String token, User user) {
    final String username = extractUsername(token);
    return (username.equals(user.username()) && !isTokenExpired(token));

  }

  public boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());

  }

  public Date extractExpiration(String token) {
    return extractClaim(token,Claims::getExpiration);
  }
}
