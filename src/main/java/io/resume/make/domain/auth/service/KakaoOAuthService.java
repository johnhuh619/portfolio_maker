package io.resume.make.domain.auth.service;

import io.resume.make.domain.auth.dto.KakaoTokenResponse;
import io.resume.make.domain.auth.exception.OAuthErrorCode;
import io.resume.make.domain.user.repository.UserRepository;
import io.resume.make.global.exception.BusinessException;
import io.resume.make.global.response.GlobalErrorCode;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

    private final UserRepository userRepository;
    private final WebClient webClient;
    private final StateManager stateManager;

    @Value("${kakao.login.api_key}")
    private String apiKey;

    @Value("${kakao.login.client_secret}")
    private String clientSecret;

    @Value("${kakao.login.uri.base}")
    private String kakaoBaseUri;

    @Value("${kakao.login.admin_key:}")
    private String adminKey;

    @Value("${oauth.kakao.redirect-uris:http://localhost:3000/callback}")
    private List<String> allowedRedirectUris;

    /**
     * 카카오 로그인 url 생성
     *
     * @return
     */
    public Map<String, String> getKakaoUrl(String redirectUri, String codeChallenge) {
        String state = stateManager.generateAndStoreState(codeChallenge);
        log.debug("State generated");

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

    /**
     * kakao 인증 code로 토큰 교환
     *
     * @param code
     * @param codeVerifier
     * @param redirectUri
     * @return
     */
    public KakaoTokenResponse exchangeKakaoToken(String code, String codeVerifier, String redirectUri) {
        String tokenUrl = UriComponentsBuilder.fromUriString(kakaoBaseUri)
                .path("/oauth/token")
                .build()
                .toUriString();

        log.info("Exchanging authorization code for token with redirect URI: {}", redirectUri);
        log.debug("Attempting token exchange");

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", apiKey);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        params.add("client_secret", clientSecret);
        params.add("code_verifier", codeVerifier);

        try {
            return webClient.post()
                    .uri(tokenUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(BodyInserters.fromFormData(params))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> {
                                        log.error("kakao token exchange failed(4xx): {}", body);
                                        return new BusinessException(OAuthErrorCode.INVALID_AUTHORIZATION_CODE);
                                    })
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> {
                                        log.error("kakao server error(5xx): {}", body);
                                        return new BusinessException(OAuthErrorCode.KAKAO_SERVER_ERROR);
                                    })
                    )
                    .bodyToMono(KakaoTokenResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .filter(throwable -> !(throwable instanceof BusinessException)))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("WebClient error during token exchange: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(OAuthErrorCode.KAKAO_TOKEN_EXCHANGE_FAILED);
        } catch (Exception e) {
            log.error("Unexpected error during token exchange: {}", e.getMessage());
            throw new BusinessException(OAuthErrorCode.KAKAO_TOKEN_EXCHANGE_FAILED);
        }

    }

    public Map<String, Object> getUserInfo(String accessToken) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                // 4xx
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> {
                                    log.error("kakao user info failed(4xx): {}", body);
                                    return new BusinessException(OAuthErrorCode.KAKAO_USER_INFO_FAILED);
                                })
                )
                // 5xx 에러 처리
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> {
                                    log.error("Kakao server error while getting user info (5xx): {}", body);
                                    return new BusinessException(OAuthErrorCode.KAKAO_SERVER_ERROR);
                                })
                )
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .timeout(Duration.ofSeconds(10))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> !(throwable instanceof BusinessException)))
                .block();
    }

    /**
     * 로그아웃(UNLINK)
     */
    public void logoutKakaoUser(String providerId) {
        if (adminKey == null || adminKey.isBlank()) {
            log.warn("Kakao admin key is not configured, skipping Kakao logout for providerId={}", providerId);
            return;
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", providerId);

        try {
            webClient.post()
                    .uri("https://kapi.kakao.com/v1/user/logout")
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .body(BodyInserters.fromFormData(params))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> {
                                        log.warn("Failed to logout Kakao user {}: {}", providerId, body);
                                        return new BusinessException(OAuthErrorCode.KAKAO_LOGOUT_FAILED);
                                    })
                    )
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
            log.info("Successfully logged out Kakao user {}", providerId);
        } catch (Exception e) {
            log.warn("Failed to logout Kakao user {}: {}", providerId, e.getMessage());
        }
    }

}
