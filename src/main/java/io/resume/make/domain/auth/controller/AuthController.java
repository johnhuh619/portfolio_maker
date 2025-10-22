package io.resume.make.domain.auth.controller;


import io.resume.make.domain.auth.dto.LoginResponse;
import io.resume.make.domain.auth.service.AuthFacadeService;
import io.resume.make.domain.auth.service.CookieManager;
import io.resume.make.domain.auth.service.KakaoOAuthService;
import io.resume.make.global.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final AuthFacadeService authFacadeService;

    @Value("${jwt.use-cookie:true}")
    private boolean useCookie;

    /**
     * kakao URL 생성
     */
    @GetMapping("/kakao/url")
    public ResponseEntity<BaseResponse<Map<String, String>>> createKakaoUrl(
        @RequestParam String redirectUri,
        @RequestParam String codeChallenge,
        HttpServletRequest request
    ) {
        log.info("Generating kakao Url with redirectUri: {}", redirectUri);
        Map<String, String>loginUrlInfo = kakaoOAuthService.getKakaoUrl(redirectUri, codeChallenge);
        return BaseResponse.ok(loginUrlInfo);
    }

    /**
     * kakao 로그인 처리
     */
    @PostMapping("/kakao/login")
    public ResponseEntity<BaseResponse<LoginResponse>> kakaoLogin(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam String codeVerifier,
            @RequestParam String redirectUri,
            HttpServletResponse response
    ) {
        log.info("Processing kakao login: code: {}, state: {}, codeVerifier: {}, redirectUri: {}", code, state, codeVerifier, redirectUri);
        LoginResponse loginResponse = authFacadeService.processKakaoLogin(code, state, codeVerifier, redirectUri, response);
        return BaseResponse.ok(loginResponse);
    }

    /**
     * refresh 토큰 refresh & blacklist 처리
     * @param request
     * @param response
     * @return 응답 결과.
     */
    @PostMapping("/refresh")
    public ResponseEntity<BaseResponse<LoginResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenBody body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = resolveRefreshToken(request, body);
        log.info("Refresh token requested via {} (present={})", useCookie ? "Cookie" : "Body", refreshToken != null);
        LoginResponse refreshed = authFacadeService.refreshToken(refreshToken, response);
        return BaseResponse.ok(refreshed);
    }

    /**
     * kakao 로그아웃 처리
     */
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenBody body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = resolveRefreshToken(request, body);
        log.info("Processing logout via {} (present={})", useCookie ? "Cookie" : "Body", refreshToken != null);
        authFacadeService.logout(refreshToken, response);
        return BaseResponse.ok(null);
    }

    public String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(CookieManager.REFRESH_TOKEN_COOKIE_NAME))
                .findFirst()
                .map(cookie -> cookie.getValue())
                .orElse(null);
    }

    private String resolveRefreshToken(HttpServletRequest request, RefreshTokenBody body) {
        if (useCookie) {
            return extractRefreshTokenFromCookies(request);
        }
        return body != null ? body.getRefreshToken() : null;
    }

    @Data
    public static class RefreshTokenBody {
        private String refreshToken;
    }
}
