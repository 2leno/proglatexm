package api.poja.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long expirationMinutes;

  public JwtTokenProvider(
      @Value("${app.security.jwt.secret:}") String secret,
      @Value("${app.security.jwt.expiration-minutes:60}") long expirationMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMinutes = expirationMinutes;
  }

  public String generateToken(String username, List<String> roles) {
    return Jwts.builder()
        .subject(username)
        .claim("roles", roles)
        .issuedAt(Date.from(Instant.now()))
        .expiration(Date.from(Instant.now().plusSeconds(expirationMinutes * 60)))
        .signWith(key)
        .compact();
  }

  public boolean isValid(String token) {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public String getUsername(String token) {
    return parseClaims(token).getSubject();
  }

  public List<String> getRoles(String token) {
    List<?> roles = parseClaims(token).get("roles", List.class);
    return roles.stream().map(String.class::cast).toList();
  }

  private Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
