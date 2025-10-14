package io.resume.make.domain.auth.service;

import io.resume.make.domain.auth.dto.KakaoTokenResponse;
import io.resume.make.domain.auth.dto.LoginResponse;
import io.resume.make.domain.user.entity.User;
import io.resume.make.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacadeService {

    private final UserRepository userRepository;
    private final KakaoOAuthService kakaoOAuthService;
    private final TokenService tokenService;

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
        KakaoTokenResponse tokenResponse = kakaoOAuthService.exchangeKakaoToken(code, codeVerifier, redirectUri);

        // 카카오 토큰으로 사용자 정보 요청 및 처리
        String accessToken = tokenResponse!= null ? tokenResponse.accessToken() : null;
        if (accessToken == null) {
            log.error("Failed to get access token from Kakao");
            throw new RuntimeException("Failed to get access token from Kakao");
        }

        // 사용자 정보 조회 및 처리
        Map<String, Object> userInfo = kakaoOAuthService.getUserInfo(accessToken);
        User user = saveOrUpdateKakaoUser(userInfo);

        // return: 로그인 사용자 서비스 토큰 생성
        return tokenService.issueTokens(user, response);
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

    public LoginResponse refreshToken(String refreshToken, HttpServletResponse response) {
        return tokenService.refreshTokens(refreshToken, response);
    }
}
