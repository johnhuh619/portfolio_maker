package io.resume.make.global.exception;

import io.resume.make.global.response.BaseResponse;
import io.resume.make.global.response.ErrorCode;
import io.resume.make.global.response.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 1. @ControllerAdvice / @RestControllerAdvice 가 붙은 빈(bean) 목록을 확인
 * 2. 해당 예외 타입(BusinessException, MissingServletRequestParameterException 등)을 처리할 수 있는 메서드가 있는지 검색
 * 3. 찾으면 그 메서드를 실행해서 ResponseEntity 를 반환받음
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 exception 처리
     * @param be
     * @return ResponseEntity: HTTP 응답 코드와 함께 BaseResponse 형태의 본문 반환
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessExceptions(BusinessException be) {
        ErrorCode ec = be.getErrorCode();
        return BaseResponse.from(ec, null);
    }

    /**
     * 요청 파라미터 누락 예외
     * @param e
     * @return ResponseEntity: HTTP 400 + 에러 메세지 반환
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingParams(MissingServletRequestParameterException e) {
        log.error("Missing parameter: {}", e.getParameterName());
        return ResponseEntity.status(GlobalErrorCode.MISSING_PARAMETER.getStatus())
                .body(BaseResponse.error(GlobalErrorCode.MISSING_PARAMETER));
        // → 400 Bad Request와 함께 "요청 파라미터 누락" 에러 객체를 JSON 형태로 반환
    }

    /**
     * 요청 파라미터 타입 불일치 예외
     * @param e
     * @return ResponseEntity: HTTP 400 + 타입 불일치 메세지 반환
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("Type mismatch: parameter={}, requiredType={}",
                 e.getName(), e.getRequiredType());
        return ResponseEntity
                .status(GlobalErrorCode.INVALID_INPUT.getStatus())
                .body(BaseResponse.error(GlobalErrorCode.INVALID_INPUT));
    }

    /**
     * Validation 실패 예외 (DTO 검증)
     * @param e
     * @return ResponseEntity: HTTP 400 + 타입 불일치 메세지 반환
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation failed: {}", e.getBindingResult().getAllErrors());
        return ResponseEntity
                .status(GlobalErrorCode.INVALID_INPUT.getStatus())
                .body(BaseResponse.error(GlobalErrorCode.INVALID_INPUT));
    }

    /**
     * 일반 예외 처리 (최후의 fallback)
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ResponseEntity
                .status(500)
                .body(BaseResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR));
    }

}
