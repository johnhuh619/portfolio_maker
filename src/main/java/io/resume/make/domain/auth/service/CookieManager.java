package io.resume.make.domain.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieManager {
    @Value("${cookie.domain}")
    private String domain;

    @Value("${cookie.secure:true}")
    private boolean secure;

    @Value("${cookie.max-age:604800")
    private Long maxAge;

    private static final String COOKIE_NAME = "refreshToken";

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(secure)
                .domain(domain)
                .sameSite("None")
                .build();
    }

    public void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
