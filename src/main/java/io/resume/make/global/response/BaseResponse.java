package io.resume.make.global.response;


import org.springframework.http.ResponseEntity;

public record BaseResponse<T>(
        String code,
        String message,
        T body
) {
    public static <T> ResponseEntity<BaseResponse<T>> from(ErrorCode error, T body) {
        return ResponseEntity
                .status(error.getStatus())
                .body(new BaseResponse<>(error.getCode(), error.getMessage(), body));
    }

    public static <T> ResponseEntity<BaseResponse<T>> ok(T body) {
        return ResponseEntity.ok(new BaseResponse<>("GLOBAL_2000", "OK", body));
    }
}
