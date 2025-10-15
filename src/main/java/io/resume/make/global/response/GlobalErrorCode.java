package io.resume.make.global.response;

import org.springframework.http.HttpStatus;

public enum GlobalErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_INPUT("GLOBAL_4001", "잘못된 입력 값입니다.", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER("GLOBAL_4002", "필수 요청 파라미터가 누락되었습니다.", HttpStatus.BAD_REQUEST),
    INVALID_REDIRECT_URI("GLOBAL_4003", "허용되지 않은 Redirect URI 입니다.", HttpStatus.BAD_REQUEST),

    // 401 Unauthorized
    UNAUTHORIZED("GLOBAL_4010", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),

    // 403 Forbidden
    FORBIDDEN("GLOBAL_4030", "권한이 없습니다.", HttpStatus.FORBIDDEN),


    // 404 Not Found
    RESOURCE_NOT_FOUND("GLOBAL_4040", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED("GLOBAL_4050", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),

    // 409 Conflict
    CONFLICT("GLOBAL_4090", "리소스 충돌이 발생했습니다.", HttpStatus.CONFLICT),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR("GLOBAL_5000", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("GLOBAL_5001", "일시적으로 서비스를 이용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_TOKEN("GLOBAL","토큰이 잘못되었습니다." , HttpStatus.BAD_REQUEST ),
    BLACKLISTED_TOKEN("GLOBAL","토큰이 이미 블랙리스트 되었습니다." , HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("GLOBAL","유저 없음" ,HttpStatus.BAD_REQUEST ),
    EXPIRED_TOKEN("GLOBAL","만료된 token" , HttpStatus.BAD_REQUEST ),;

    private final String code;
    private final String message;
    private final HttpStatus status;

    GlobalErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

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
