package io.resume.make.domain.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.resume.make.domain.user.entity.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

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
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenExpirationTime);


        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("token_type", "access")
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenValiditySeconds);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("token_type", "refresh")
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }
}
