package io.resume.make.global.response;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String getCode();
    String getMessage();
    HttpStatus getStatus();
}
