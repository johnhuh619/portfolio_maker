package io.resume.make.domain.user.service;

import io.resume.make.domain.user.repository.UserRepository;
import io.resume.make.global.exception.BusinessException;
import io.resume.make.global.response.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    @Value("${kakao.login.api_key}")
    private String apiKey;

    @Value("${kakao.login.client_secret}")
    private String clientSecret;

    @Value("${kakao.login.uri.base}")
    private String kakaoBaseUri;

    @Value("${oauth.kakao.redirect-uris}")
    private List<String> allowedRedirectUris;

    /**
     * 카카오 로그인 처리
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

    /**
     * 토큰 재발급
     */
    //TODO

    /**
     * 로그아웃
     */
    //TODO
}
