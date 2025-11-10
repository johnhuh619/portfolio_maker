package io.resume.make.domain.auth.filter;

import io.resume.make.domain.auth.jwt.JwtTokenProvider;
import io.resume.make.domain.user.entity.User;
import io.resume.make.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 단위 테스트")
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Test
    @DisplayName("유효한 JWT 토큰 - 인증 성공")
    void doFilterInternal_ValidToken_AuthenticationSuccess() throws Exception {
        // given
        String token = "valid-jwt-token";
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .provider("kakao")
                .providerId("123456")
                .email("test@example.com")
                .name("테스트유저")
                .build();

        given(request.getHeader("Authorization")).willReturn("Bearer " + token);
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.extractUserId(token)).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(user);
        assertThat(authentication.isAuthenticated()).isTrue();

        verify(filterChain).doFilter(request, response);

        // Clean up
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("OAuth 경로는 필터 제외 - /auth/kakao/url")
    void shouldNotFilter_OAuthUrlPath_ReturnsTrue() throws Exception {
        // given
        given(request.getRequestURI()).willReturn("/auth/kakao/url");

        // when
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("보호된 API 경로는 필터 적용")
    void shouldNotFilter_ProtectedPath_ReturnsFalse() throws Exception {
        // given
        given(request.getRequestURI()).willReturn("/api/profile");

        // when
        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        // then
        assertThat(result).isFalse();
    }
}
