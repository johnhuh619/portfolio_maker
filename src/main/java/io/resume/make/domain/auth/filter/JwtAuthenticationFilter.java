package io.resume.make.domain.auth.filter;

import io.resume.make.domain.auth.jwt.JwtTokenProvider;
import io.resume.make.domain.user.entity.User;
import io.resume.make.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Authorization 헤더에서 Jwt token 추출
            String token = extractTokenFromRequest(request);

            // 2. token 있고 valid
            if (token != null && jwtTokenProvider.validateToken(token) && jwtTokenProvider.hasTokenType(token, "access")) {
                // 3. token 에서 user id extract
                UUID userId = jwtTokenProvider.extractUserId(token);

                // 4. find User
                User user = userRepository.findById(userId).orElse(null);

                if (user != null) {
                    // 5. Spring Security 인증 객체 생성
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 6. SecurityContext에 인증 정보 설정
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.debug("Set authentication to SecurityContext: {}", authenticationToken);
                    log.debug("Set authentication for User: {}", userId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return path.startsWith("/auth/kakao/url") ||
                path.startsWith("/auth/kakao/login") ||
                path.startsWith("/h2-console");
    }
}
