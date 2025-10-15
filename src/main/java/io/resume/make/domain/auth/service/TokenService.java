package io.resume.make.domain.auth.service;

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
        String jwtAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail() != null ? user.getEmail() : user.getName());
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail() != null ? user.getEmail() : user.getName());
        cookieManager.addCookie(response, cookieManager.createRefreshTokenCookie(jwtRefreshToken));

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
        if (blacklistedTokenRepository.existsByRefreshToken(refreshToken)) {
            log.error("Refresh token is blacklisted");
            throw new BusinessException(GlobalErrorCode.BLACKLISTED_TOKEN);
        }

        // 2. 토큰 검증
        if (!jwtTokenProvider.validateToken(refreshToken)){
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
        cookieManager.addCookie(response, cookieManager.createRefreshTokenCookie(newRefreshToken));
        log.debug("Using cookie for refresh token");

        // 5. 기존 토큰 blacklist 추가
        addToBlacklist(refreshToken, userId);
        log.info("Refresh token added to blacklist: userId: {}, expiresAt: {}", userId, jwtTokenProvider.getExpirationDate(refreshToken));
        return LoginResponse.of(user, newAccessToken, newRefreshToken);
    }

    public void revokeRefreshToken(String refreshToken, HttpServletResponse response) {
        cookieManager.removeCookie(response);

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("No refresh token provided for logout");
            return;
        }

        if (blacklistedTokenRepository.existsByRefreshToken(refreshToken)) {
            log.info("Refresh token already blacklisted");
            return;
        }

        if (!jwtTokenProvider.validateToken(refreshToken)){
            log.warn("Invalid refresh token provided during logout");
            return;
        }

        UUID userId = jwtTokenProvider.extractUserId(refreshToken);
        addToBlacklist(refreshToken, userId);
        log.info("Refresh token revoked for userId: {}", userId);
    }

    public void blacklistRefreshToken(String refreshToken, UUID userId, LocalDateTime expiresAt) {
        blacklistedTokenRepository.save(BlacklistedRefreshToken.builder()
                .userId(userId)
                .refreshToken(refreshToken)
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
}
