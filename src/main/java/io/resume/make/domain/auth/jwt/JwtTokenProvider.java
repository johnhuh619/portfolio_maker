package io.resume.make.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("$${jwt.secret")
    private String secret;

    @Value("${jwt.access-token-validity-seconds}")
    private Long accessTokenValiditySeconds = 300L;

    @Value("${jwt.access-token-expiration-time}")
    private Long accessTokenExpirationTime = 1000 * 60 * 60 * 10L;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public String generateAccessToken(UUID userId, String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenExpirationTime);


        return Jwts.builder()
                .subject(userId.toString())
                .claim("token_type", "access")
                .claim("userId", userId)
                .claim("email", email)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenValiditySeconds);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("token_type", "refresh")
                .claim("userId", userId)
                .claim("email", email)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public UUID extractUserId(String token) {
        try {
            Jws<Claims> jwt = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            String idString = jwt.getPayload().get("userId", String.class);
            return UUID.fromString(idString);
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT Token while getting userId: {}", e.getMessage());
            throw new IllegalArgumentException("Expired JWT Token while getting userId");
        } catch (Exception e) {
            log.error("Invalid JWT Token while getting userId: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT Token while getting userId");
        }
    }
}
