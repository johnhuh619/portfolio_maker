package io.resume.make.domain.auth.exception;

import io.resume.make.global.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OAuthErrorCode implements ErrorCode {

    // State/PKCE 관련
    INVALID_STATE("OAUTH_4004", "유효하지 않거나 만료된 State 값입니다.", HttpStatus.BAD_REQUEST),
    INVALID_CODE_VERIFIER("OAUTH_4005", "PKCE 검증에 실패했습니다.", HttpStatus.BAD_REQUEST),

    // 카카오 API 관련 에러
    KAKAO_TOKEN_EXCHANGE_FAILED("OAUTH_4001", "카카오 토큰 교환에 실패했습니다.", HttpStatus.BAD_REQUEST),
    KAKAO_USER_INFO_FAILED("OAUTH_4002", "카카오 사용자 정보 조회에 실패했습니다.", HttpStatus.BAD_REQUEST),
    KAKAO_LOGOUT_FAILED("OAUTH_4003", "카카오 로그아웃에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    KAKAO_API_TIMEOUT("OAUTH_5001", "카카오 API 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    KAKAO_SERVER_ERROR("OAUTH_5002", "카카오 서버 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),

    // 일반 OAuth 에러
    INVALID_AUTHORIZATION_CODE("OAUTH_4006", "유효하지 않은 인증 코드입니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }
}