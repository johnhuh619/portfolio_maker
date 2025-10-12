package io.resume.make.domain.user.service;

import io.resume.make.domain.user.dto.KakaoTokenResponse;
import io.resume.make.domain.user.dto.LoginResponse;
import io.resume.make.domain.user.repository.UserRepository;
import io.resume.make.global.exception.BusinessException;
import io.resume.make.global.response.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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


    /**
     * @param code
     * @param state
     * @param codeVerifier
     * @param redirectUri
     * @param response
     * @return
     */
    public LoginResponse processKakaoLogin(String code, String state, String codeVerifier, String redirectUri, HttpServletRequest response) {
        log.info("processing kakao login: code: {}, state: {}, codeVerifier: {}, redirectUri: {}", code, state, codeVerifier, redirectUri);
        KakaoTokenResponse tokenResponse = exchangeKakaoToken(code, codeVerifier, redirectUri);

        String accessToken = tokenResponse!= null ? tokenResponse.accessToken() : null;
        if (accessToken == null) {
            log.error("Failed to get access token from Kakao");
            throw new RuntimeException("Failed to get access token from Kakao");
        }

    }


    /**
     * 토큰 재발급
     */
    //TODO

    /**
     * 로그아웃
     */
    //TODO
}
