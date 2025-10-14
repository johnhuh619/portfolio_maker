package io.resume.make.domain.auth.service;

import io.resume.make.domain.auth.dto.KakaoTokenResponse;
import io.resume.make.domain.auth.dto.LoginResponse;
import io.resume.make.domain.auth.jwt.JwtTokenProvider;
import io.resume.make.domain.user.entity.User;
import io.resume.make.domain.user.repository.UserRepository;
import io.resume.make.global.exception.BusinessException;
import io.resume.make.global.response.GlobalErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WebClient webClient;
    private final CookieManager cookieManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${kakao.login.api_key}")
    private String apiKey;

    @Value("${kakao.login.client_secret}")
    private String clientSecret;

    @Value("${kakao.login.uri.base}")
    private String kakaoBaseUri;

    @Value("${oauth.kakao.redirect-uris}")
    private List<String> allowedRedirectUris;

    /**
     * 카카오 로그인 url 생성
     *
     * @return
     */
    public Map<String, String> getKakaoUrl(String redirectUri, String codeChallenge) {
        String state = UUID.randomUUID().toString();
        log.info("State: {}", state);

        if (!allowedRedirectUris.contains(redirectUri)) {
            throw new BusinessException(GlobalErrorCode.INVALID_REDIRECT_URI);
        }

        String kakaoAuthUrl = UriComponentsBuilder.fromUriString(kakaoBaseUri)
                .path("/oauth/authorize")
                .queryParam("client_id", apiKey)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build().toUriString();
        return Map.of(
                "loginUrl", kakaoAuthUrl,
                "state", state
        );
    }

    public KakaoTokenResponse exchangeKakaoToken(String code, String codeVerifier, String redirectUri) {
        String tokenUrl = UriComponentsBuilder.fromUriString(kakaoBaseUri)
                .path("/oauth/token")
                .build()
                .toUriString();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", apiKey);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        params.add("client_secret", clientSecret);
        params.add("code_verifier", codeVerifier);

        return webClient.post()
                .uri(tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData(params))
                .retrieve().bodyToMono(KakaoTokenResponse.class)
                .block();
    }

    public Map<String, Object> getUserInfo(String accessToken) {
        return webClient.get()
                .uri("http://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();
    }


    public User processKakaoUser(String accessToken) {
        Map<String, Object> userInfo = getUserInfo(accessToken);
        return saveOrUpdateKakaoUser(userInfo);
    }

    private User saveOrUpdateKakaoUser(Map<String, Object> userInfo) {
        Long providerId = (Long) userInfo.get("id");
        Map<String, Object> account = (Map<String, Object>)userInfo.get("kakao_account");
        String name = (String) account.get("nickname");
        String email = (String) account.get("email");

        User user = userRepository.findByProviderAndProviderId("kakao", String.valueOf(providerId))
                .orElseGet(() -> User.builder()
                        .provider("kakao")
                        .providerId(String.valueOf(providerId))
                        .email(email)
                        .name(name)
                        .build()
                );
        user.updateName(name);
        return userRepository.save(user);
    }

    /** 카카오 로그인 처리
     * @param code code
     * @param state state
     * @param codeVerifier front에서 줘야 하는 pkce
     * @param redirectUri redirecturi
     * @param response 로그인 응답 dto
     * @return
     */
    public LoginResponse processKakaoLogin(String code, String state, String codeVerifier, String redirectUri, HttpServletResponse response) {
        log.info("processing kakao login: code: {}, state: {}, codeVerifier: {}, redirectUri: {}", code, state, codeVerifier, redirectUri);
        KakaoTokenResponse tokenResponse = exchangeKakaoToken(code, codeVerifier, redirectUri);

        // 카카오 토큰으로 사용자 정보 요청 및 처리
        String accessToken = tokenResponse!= null ? tokenResponse.accessToken() : null;
        if (accessToken == null) {
            log.error("Failed to get access token from Kakao");
            throw new RuntimeException("Failed to get access token from Kakao");
        }

        // 사용자 정보 조회 및 처리
        User user = processKakaoUser(accessToken);

        // 로그인 사용자 서비스 토큰 생성
        String jwtAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail() != null ? user.getEmail() : user.getName());
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail() != null ? user.getEmail() : user.getName());

        cookieManager.addCookie(response, cookieManager.createRefreshTokenCookie(jwtRefreshToken));
        return LoginResponse.of(user, jwtAccessToken, jwtRefreshToken);
    }


    /**
     * 토큰 재발급
     */
    public LoginResponse refreshToken(String refreshToken, HttpServletResponse response) {
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

    /**
     * 로그아웃
     */
    //TODO
}
