package io.resume.make.domain.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CookieManager {
    @Value("${cookie.domain}")
    private String domain;

    @Value("${cookie.secure:true}")
    private boolean secure;

    @Value("${cookie.max-age:604800}")
    private Long maxAge;

    @Value("${jwt.use-cookie:true}")
    private boolean useCookie;

    @Value("${cookie.same-site:Lax}")
    private String sameSite;

    @Value("${cookie.http-only:true}")
    private boolean httpOnly;

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    public ResponseCookie createRefreshTokenCookie(String refreshToken, Long maxAgeSeconds) {
        return createBaseBuilder(refreshToken, maxAgeSeconds)
                .build();
    }

    public void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        if (useCookie) {
            response.addHeader("Set-Cookie", cookie.toString());

        } else {
            log.debug("Skipping cookie (use-cookie=false)");
        }
    }

    public void removeCookie(HttpServletResponse response) {
        if (!useCookie) {
            return;
        }
        ResponseCookie expiredCookie = createBaseBuilder("", 0)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", expiredCookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder createBaseBuilder(String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
                .path("/")
                .maxAge(maxAgeSeconds)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite);
        if (domain != null && !domain.isBlank() && !"localhost".equalsIgnoreCase(domain)) {
            builder.domain(domain);
        }
        return builder;
    }
}
