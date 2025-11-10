package io.resume.make.domain.auth.service;

import io.resume.make.domain.auth.dto.KakaoTokenResponse;
import io.resume.make.domain.auth.dto.LoginResponse;
import io.resume.make.domain.auth.exception.OAuthErrorCode;
import io.resume.make.domain.user.entity.User;
import io.resume.make.domain.user.repository.UserRepository;
import io.resume.make.global.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthFacadeService 단위 테스트")
class AuthFacadeServiceTest {

    @InjectMocks
    private AuthFacadeService authFacadeService;

    @Mock
    private StateManager stateManager;

    @Mock
    private KakaoOAuthService kakaoOAuthService;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("카카오 로그인 성공 - 신규 사용자")
    void processKakaoLogin_NewUser_Success() throws Exception {
        // given
        String code = "auth-code";
        String state = "test-state";
        String codeVerifier = "test-verifier";
        String redirectUri = "http://localhost:3000/callback";

        // PKCE 검증을 위한 codeChallenge 생성
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        String expectedCodeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        KakaoTokenResponse tokenResponse = new KakaoTokenResponse(
                "kakao-access-token",
                "Bearer",
                "kakao-refresh-token",
                3600L,
                86400L,
                null
        );

        Map<String, Object> userInfo = Map.of(
                "id", 123456789L,
                "kakao_account", Map.of(
                        "email", "test@example.com",
                        "profile", Map.of("nickname", "테스트유저")
                )
        );

        User newUser = User.builder()
                .provider("kakao")
                .providerId("123456789")
                .email("test@example.com")
                .name("테스트유저")
                .build();

        LoginResponse expectedResponse = LoginResponse.builder()
                .accessToken("jwt-access-token")
                .userId(UUID.randomUUID())
                .email("test@example.com")
                .nickname("테스트유저")
                .refreshToken("jwt-refresh-token")
                .build();

        given(stateManager.validateAndConsumeState(state)).willReturn(expectedCodeChallenge);
        given(kakaoOAuthService.exchangeKakaoToken(code, codeVerifier, redirectUri)).willReturn(tokenResponse);
        given(kakaoOAuthService.getUserInfo("kakao-access-token")).willReturn(userInfo);
        given(userRepository.findByProviderAndProviderId("kakao", "123456789")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(newUser);
        given(tokenService.issueTokens(any(User.class), eq(response))).willReturn(expectedResponse);

        // when
        LoginResponse result = authFacadeService.processKakaoLogin(code, state, codeVerifier, redirectUri, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("jwt-access-token");
        assertThat(result.refreshToken()).isEqualTo("jwt-refresh-token");

        verify(stateManager).validateAndConsumeState(state);
        verify(kakaoOAuthService).exchangeKakaoToken(code, codeVerifier, redirectUri);
        verify(kakaoOAuthService).getUserInfo("kakao-access-token");
        verify(userRepository).save(any(User.class));
        verify(tokenService).issueTokens(any(User.class), eq(response));
    }

    @Test
    @DisplayName("카카오 로그인 실패 - 잘못된 State")
    void processKakaoLogin_InvalidState_ThrowsException() {
        // given
        String code = "auth-code";
        String invalidState = "invalid-state";
        String codeVerifier = "test-verifier";
        String redirectUri = "http://localhost:3000/callback";

        given(stateManager.validateAndConsumeState(invalidState))
                .willThrow(new BusinessException(OAuthErrorCode.INVALID_STATE));

        // when & then
        assertThatThrownBy(() ->
                authFacadeService.processKakaoLogin(code, invalidState, codeVerifier, redirectUri, response)
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OAuthErrorCode.INVALID_STATE);

        verify(stateManager).validateAndConsumeState(invalidState);
        verify(kakaoOAuthService, never()).exchangeKakaoToken(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("카카오 로그인 실패 - PKCE 검증 실패")
    void processKakaoLogin_InvalidPKCE_ThrowsException() {
        // given
        String code = "auth-code";
        String state = "test-state";
        String wrongCodeVerifier = "wrong-verifier";
        String redirectUri = "http://localhost:3000/callback";

        String storedCodeChallenge = "different-challenge";

        given(stateManager.validateAndConsumeState(state)).willReturn(storedCodeChallenge);

        // when & then
        assertThatThrownBy(() ->
                authFacadeService.processKakaoLogin(code, state, wrongCodeVerifier, redirectUri, response)
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OAuthErrorCode.INVALID_CODE_VERIFIER);

        verify(stateManager).validateAndConsumeState(state);
        verify(kakaoOAuthService, never()).exchangeKakaoToken(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("카카오 로그인 실패 - 액세스 토큰 null")
    void processKakaoLogin_NullAccessToken_ThrowsException() throws Exception {
        // given
        String code = "auth-code";
        String state = "test-state";
        String codeVerifier = "test-verifier";
        String redirectUri = "http://localhost:3000/callback";

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        String expectedCodeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        KakaoTokenResponse tokenResponse = new KakaoTokenResponse(
                null,  // accessToken이 null
                "Bearer",
                "kakao-refresh-token",
                3600L,
                86400L,
                null
        );

        given(stateManager.validateAndConsumeState(state)).willReturn(expectedCodeChallenge);
        given(kakaoOAuthService.exchangeKakaoToken(code, codeVerifier, redirectUri)).willReturn(tokenResponse);

        // when & then
        assertThatThrownBy(() ->
                authFacadeService.processKakaoLogin(code, state, codeVerifier, redirectUri, response)
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OAuthErrorCode.KAKAO_TOKEN_EXCHANGE_FAILED);

        verify(kakaoOAuthService, never()).getUserInfo(anyString());
    }

    @Test
    @DisplayName("카카오 로그인 성공 - 기존 사용자 업데이트")
    void processKakaoLogin_ExistingUser_Success() throws Exception {
        // given
        String code = "auth-code";
        String state = "test-state";
        String codeVerifier = "test-verifier";
        String redirectUri = "http://localhost:3000/callback";

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        String expectedCodeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

        KakaoTokenResponse tokenResponse = new KakaoTokenResponse(
                "kakao-access-token",
                "Bearer",
                "kakao-refresh-token",
                3600L,
                86400L,
                null
        );

        Map<String, Object> userInfo = Map.of(
                "id", 123456789L,
                "kakao_account", Map.of(
                        "email", "test@example.com",
                        "profile", Map.of("nickname", "업데이트된닉네임")
                )
        );

        User existingUser = User.builder()
                .provider("kakao")
                .providerId("123456789")
                .email("test@example.com")
                .name("기존닉네임")
                .build();

        LoginResponse expectedResponse = LoginResponse.builder()
                .accessToken("jwt-access-token")
                .userId(UUID.randomUUID())
                .email("test@example.com")
                .nickname("업데이트된닉네임")
                .refreshToken("jwt-refresh-token")
                .build();

        given(stateManager.validateAndConsumeState(state)).willReturn(expectedCodeChallenge);
        given(kakaoOAuthService.exchangeKakaoToken(code, codeVerifier, redirectUri)).willReturn(tokenResponse);
        given(kakaoOAuthService.getUserInfo("kakao-access-token")).willReturn(userInfo);
        given(userRepository.findByProviderAndProviderId("kakao", "123456789")).willReturn(Optional.of(existingUser));
        given(userRepository.save(any(User.class))).willReturn(existingUser);
        given(tokenService.issueTokens(any(User.class), eq(response))).willReturn(expectedResponse);

        // when
        LoginResponse result = authFacadeService.processKakaoLogin(code, state, codeVerifier, redirectUri, response);

        // then
        assertThat(result).isNotNull();
        verify(userRepository).save(argThat(user ->
                user.getName().equals("업데이트된닉네임")
        ));
    }
}
