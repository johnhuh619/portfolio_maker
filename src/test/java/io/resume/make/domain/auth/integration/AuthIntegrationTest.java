package io.resume.make.domain.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.resume.make.domain.auth.exception.OAuthErrorCode;
import io.resume.make.domain.auth.service.StateManager;
import io.resume.make.domain.user.repository.UserRepository;
import io.resume.make.global.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OAuth 인증 통합 테스트")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StateManager stateManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("카카오 로그인 URL 생성 - 성공")
    void createKakaoUrl_Success() throws Exception {
        // given
        String redirectUri = "http://localhost:3000/callback";
        String codeChallenge = "test-code-challenge-12345";

        // when & then
        MvcResult result = mockMvc.perform(get("/auth/kakao/url")
                        .param("redirectUri", redirectUri)
                        .param("codeChallenge", codeChallenge)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("GLOBAL_2000"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.body.loginUrl").exists())
                .andExpect(jsonPath("$.body.state").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        Map<String, String> body = (Map<String, String>) response.get("body");

        String loginUrl = body.get("loginUrl");
        String state = body.get("state");

        assertThat(loginUrl).contains("https://kauth.kakao.com/oauth/authorize");
        assertThat(loginUrl).contains("state=" + state);
        assertThat(loginUrl).contains("code_challenge=" + codeChallenge);
        assertThat(state).isNotNull();

        // Redis에 저장되었는지 확인
        String key = "oauth:state:" + state;
        String storedChallenge = redisTemplate.opsForValue().get(key);
        assertThat(storedChallenge).isEqualTo(codeChallenge);
    }

    @Test
    @DisplayName("카카오 로그인 URL 생성 - 잘못된 redirectUri")
    void createKakaoUrl_InvalidRedirectUri_Returns400() throws Exception {
        // given
        String invalidRedirectUri = "http://malicious-site.com/callback";
        String codeChallenge = "test-code-challenge";

        // when & then
        mockMvc.perform(get("/auth/kakao/url")
                        .param("redirectUri", invalidRedirectUri)
                        .param("codeChallenge", codeChallenge)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_4003"))
                .andExpect(jsonPath("$.message").value("허용되지 않은 Redirect URI 입니다."));
    }

    @Test
    @DisplayName("State 재사용 방지 검증")
    void stateReusePrevention_ThrowsException() throws Exception {
        // given
        String codeVerifier = "test-verifier-123";
        String codeChallenge = computeCodeChallenge(codeVerifier);
        String state = stateManager.generateAndStoreState(codeChallenge);

        // 첫 번째 사용 (성공)
        stateManager.validateAndConsumeState(state);

        // when & then - 두 번째 사용 (실패)
        assertThatThrownBy(() -> stateManager.validateAndConsumeState(state))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OAuthErrorCode.INVALID_STATE);
    }

    @Test
    @DisplayName("PKCE 검증 - SHA-256 해시 일치")
    void pkceValidation_SHA256Match() throws Exception {
        // given
        String codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String expectedCodeChallenge = computeCodeChallenge(codeVerifier);

        String state = stateManager.generateAndStoreState(expectedCodeChallenge);
        String retrievedChallenge = stateManager.validateAndConsumeState(state);

        // when
        String computedChallenge = computeCodeChallenge(codeVerifier);

        // then
        assertThat(computedChallenge).isEqualTo(retrievedChallenge);
    }

    // Helper method
    private String computeCodeChallenge(String codeVerifier) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }
}
