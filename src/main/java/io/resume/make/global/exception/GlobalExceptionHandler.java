package io.resume.make.global.exception;

import io.resume.make.global.response.BaseResponse;
import io.resume.make.global.response.ErrorCode;
import io.resume.make.global.response.GlobalErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessExceptions(BusinessException be) {
        ErrorCode ec = be.getErrorCode();
        return BaseResponse.from(ec, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleExceptions(Exception e) {
        ErrorCode ec = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        return BaseResponse.from(ec, null);
    }


}
