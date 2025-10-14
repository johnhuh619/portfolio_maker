package io.resume.make.domain.auth.service;

import io.resume.make.domain.auth.dto.LoginResponse;
import io.resume.make.domain.auth.jwt.JwtTokenProvider;
import io.resume.make.domain.user.entity.User;
import io.resume.make.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final CookieManager cookieManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public LoginResponse issueTokens(User user, HttpServletResponse response) {
        String jwtAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail() != null ? user.getEmail() : user.getName());
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail() != null ? user.getEmail() : user.getName());
        cookieManager.addCookie(response, cookieManager.createRefreshTokenCookie(jwtRefreshToken));

        return LoginResponse.of(user, jwtAccessToken, jwtRefreshToken);
    }

    /**
     * 토큰 재발급
     */
    public LoginResponse refreshTokens(String refreshToken, HttpServletResponse response) {
        if(refreshToken == null || refreshToken.isEmpty() ) {
            log.error("refreshToken is null");
            throw new RuntimeException("refreshToken is null");
        }

        // 2. 리프레시 토큰 valid

        // 3. 토큰에서 사용자 ID 추출 및 회원 조회
        UUID userId = jwtTokenProvider.extractUserId(refreshToken);
        log.info("Refreshing token for userId: {}", userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // 4. 기존 토큰 blacklist 추가

        // 5. 새 토큰 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, user.getEmail());
        cookieManager.addCookie(response, cookieManager.createRefreshTokenCookie(newRefreshToken));
        log.debug("Using cookie for refresh token");
        return LoginResponse.of(user, newAccessToken, newRefreshToken);
    }
}
