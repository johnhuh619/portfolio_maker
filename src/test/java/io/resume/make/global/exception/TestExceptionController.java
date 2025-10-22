package io.resume.make.global.exception;

import io.resume.make.domain.auth.exception.OAuthErrorCode;
import io.resume.make.global.response.GlobalErrorCode;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.bind.annotation.*;

@TestComponent
@RestController
@RequestMapping("/test")
public class TestExceptionController {

    @GetMapping("/oauth-error")
    public void throwOAuthError() {
        throw new BusinessException(OAuthErrorCode.INVALID_STATE);
    }

    @GetMapping("/global-error")
    public void throwGlobalError() {
        throw new BusinessException(GlobalErrorCode.INVALID_INPUT);
    }

    @GetMapping("/type-mismatch")
    public void typeMismatch(@RequestParam Integer id) {
        // 파라미터 타입 불일치 발생
    }

    @GetMapping("/general-error")
    public void throwGeneralError() {
        throw new RuntimeException("Unexpected error");
    }
}
