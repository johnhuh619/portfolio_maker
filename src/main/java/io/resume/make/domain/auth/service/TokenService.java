package io.resume.make.domain.auth.service;

import static java.time.Duration.*;

import io.resume.make.domain.auth.dto.LoginResponse;
import io.resume.make.domain.auth.entity.BlacklistedRefreshToken;
import io.resume.make.domain.auth.jwt.JwtTokenProvider;
import io.resume.make.domain.auth.repository.BlacklistedTokenRepository;
import io.resume.make.domain.user.entity.User;
import io.resume.make.domain.user.repository.UserRepository;
import io.resume.make.global.exception.BusinessException;
import io.resume.make.global.response.GlobalErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final CookieManager cookieManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    public LoginResponse issueTokens(User user, HttpServletResponse response) {
        String email = user.getEmail();
        String jwtAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), email);
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), email);
        long maxAge = between(LocalDateTime.now(), jwtTokenProvider.getExpirationDate(jwtRefreshToken)).getSeconds();
        cookieManager.addCookie(response, cookieManager.createRefreshTokenCookie(jwtRefreshToken, maxAge));

        return LoginResponse.of(user, jwtAccessToken, jwtRefreshToken);
    }

    /**
     * 리프레시 토큰 재발급
     */
    public LoginResponse refreshTokens(String refreshToken, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.error("refreshToken is null");
            throw new BusinessException(GlobalErrorCode.INVALID_TOKEN);
        }

        // 1. 블랙리스트에 있으면 무효
        byte[] tokenHash = hashToken(refreshToken);
        if (blacklistedTokenRepository.existsByTokenHash(tokenHash)) {
            log.error("Refresh token is blacklisted");
            throw new BusinessException(GlobalErrorCode.BLACKLISTED_TOKEN);
        }

        // 2. 토큰 검증 및 타입 체크
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.hasTokenType(refreshToken, "refresh")){
            log.error("Invalid refresh token");
            throw new BusinessException(GlobalErrorCode.EXPIRED_TOKEN);
        }

        // 3. 토큰에서 사용자 ID 추출 및 회원 조회
        UUID userId = jwtTokenProvider.extractUserId(refreshToken);
        log.info("Refreshing token for userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.USER_NOT_FOUND));

        // 4. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, user.getEmail());
        long maxAge = between(LocalDateTime.now(), jwtTokenProvider.getExpirationDate(newRefreshToken)).getSeconds();
        cookieManager.addCookie(response, cookieManager.createRefreshTokenCookie(newRefreshToken, maxAge));
        log.debug("Using cookie for refresh token");

        // 5. 기존 토큰 blacklist 추가
        addToBlacklist(refreshToken, userId);
        log.info("Refresh token added to blacklist: userId: {}, expiresAt: {}", userId, jwtTokenProvider.getExpirationDate(refreshToken));
        return LoginResponse.of(user, newAccessToken, newRefreshToken);
    }

    public UUID revokeRefreshToken(String refreshToken, HttpServletResponse response) {
        cookieManager.removeCookie(response);

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("No refresh token provided for logout");
            return null;
        }

        byte[] tokenHash = hashToken(refreshToken);
        if (blacklistedTokenRepository.existsByTokenHash(tokenHash)) {
            log.info("Refresh token already blacklisted");
            return null;
        }

        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.hasTokenType(refreshToken, "refresh")){
            log.warn("Invalid refresh token provided during logout");
            return null;
        }

        UUID userId = jwtTokenProvider.extractUserId(refreshToken);
        addToBlacklist(refreshToken, userId);
        log.info("Refresh token revoked for userId: {}", userId);
        return userId;
    }

    public void blacklistRefreshToken(String refreshToken, UUID userId, LocalDateTime expiresAt) {
        byte[] tokenHash = hashToken(refreshToken);
        blacklistedTokenRepository.save(BlacklistedRefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiredAt(expiresAt)
                .build());
    }

    public void addToBlacklist(String refreshToken, UUID userId) {
        LocalDateTime expiresAt = jwtTokenProvider.getExpirationDate(refreshToken);
        log.info("Adding refresh token to blacklist: userId: {}, expiresAt: {}", userId, expiresAt);
        blacklistRefreshToken(refreshToken, userId, expiresAt);
    }

    public void cleanupExpiredBlacklist() {
        blacklistedTokenRepository.deleteAllByExpiredAtBefore(LocalDateTime.now());
        log.info("Cleanup expired blacklist tokens");
    }

    /**
     * 토큰을 SHA-256으로 해시
     */
    private byte[] hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash token: {}", e.getMessage());
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
