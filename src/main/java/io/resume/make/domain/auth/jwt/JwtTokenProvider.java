package io.resume.make.domain.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-time}")
    private long accessTokenExpirationTime;

    @Value("${jwt.refresh-token-expiration-time}")
    private long refreshTokenExpirationTime;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters for HMAC signing");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email) {
        return generateToken(userId, email, "access", accessTokenExpirationTime);
    }

    public String generateRefreshToken(UUID userId, String email) {
        return generateToken(userId, email, "refresh", refreshTokenExpirationTime);
    }

    public String generateToken(UUID userId, String email, String type, Long expirationMills) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMills);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("token_type", type)
                .claim("userId", userId.toString())
                .claim("email", email)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /**
     * token 에서 userId 추출
     *
     * @param token
     * @return userId
     */
    public UUID extractUserId(String token) {
        try {
            Claims claims = getClaims(token);
            String idString = claims.getSubject();
            return UUID.fromString(idString);
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT Token while getting userId: {}", e.getMessage());
            throw new IllegalArgumentException("Expired JWT Token while getting userId");
        } catch (Exception e) {
            log.error("Invalid JWT Token while getting userId: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT Token while getting userId");
        }
    }

    /**
     * claims 추출
     *
     * @param token 토큰
     * @return token 에서 추출한 claim
     */
    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.warn("Invalid JWT Token while getting claims: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT Token while getting claims");
        }
    }

    public LocalDateTime getExpirationDate(String token) {
        Claims claims = getClaims(token);
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            throw new IllegalArgumentException("JWT Token has no expiration date");
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(expiration.getTime()),
                ZoneId.systemDefault()
        );
    }


    public boolean validateToken(String refreshToken) {
        try {
            Jws<Claims> jwt = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(refreshToken);
            return jwt.getBody().getExpiration().after(new Date());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT Token while validating token: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("Invalid JWT Token while validating token: {}", e.getMessage());
            return false;
        }

    }
}
