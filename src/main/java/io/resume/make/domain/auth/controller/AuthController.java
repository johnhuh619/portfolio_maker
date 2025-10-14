package io.resume.make.domain.auth.controller;


import io.resume.make.domain.auth.service.KakaoOAuthService;
import io.resume.make.global.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoOAuthService kakaoOAuthService;

    /**
     * kakao URL 생성
     */
    @GetMapping("/kakao/url")
    public ResponseEntity<BaseResponse<Map<String, String>>> createKakaoUrl(
        @RequestParam String redirectUri,
        @RequestParam String codeChallenge,
        HttpServletRequest request
    ) {
        log.info("Generating kakao Url with redirectUri: {}", redirectUri);
        Map<String, String>loginUrlInfo = kakaoOAuthService.getKakaoUrl(redirectUri, codeChallenge);
        return BaseResponse.ok(loginUrlInfo);
    }

    /**
     * kakao 로그인 처리
     */


}
