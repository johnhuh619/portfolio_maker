package io.resume.make.global.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestExceptionController.class)
@DisplayName("GlobalExceptionHandler 통합 테스트")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("BusinessException 처리 - OAuthErrorCode")
    void handleBusinessException_OAuthError() throws Exception {
        // when & then
        mockMvc.perform(get("/test/oauth-error"))
                .andDo(result -> {
                    System.out.println("Status: " + result.getResponse().getStatus());
                    System.out.println("Response: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OAUTH_4004"))
                .andExpect(jsonPath("$.message").value("유효하지 않거나 만료된 State 값입니다."))
                .andExpect(jsonPath("$.body").isEmpty());
    }

    @Test
    @DisplayName("BusinessException 처리 - GlobalErrorCode")
    void handleBusinessException_GlobalError() throws Exception {
        // when & then
        mockMvc.perform(get("/test/global-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_4001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력 값입니다."))
                .andExpect(jsonPath("$.body").isEmpty());
    }

    @Test
    @DisplayName("MissingServletRequestParameterException 처리")
    void handleMissingParams() throws Exception {
        // when & then - redirectUri 파라미터 누락
        mockMvc.perform(get("/auth/kakao/url")
                        .param("codeChallenge", "test-challenge"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_4002"))
                .andExpect(jsonPath("$.message").value("필수 요청 파라미터가 누락되었습니다."))
                .andExpect(jsonPath("$.body").isEmpty());
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException 처리")
    void handleTypeMismatch() throws Exception {
        // when & then
        mockMvc.perform(get("/test/type-mismatch")
                        .param("id", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GLOBAL_4001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력 값입니다."))
                .andExpect(jsonPath("$.body").isEmpty());
    }

    @Test
    @DisplayName("일반 Exception 처리 - 500 Internal Server Error")
    void handleGeneralException() throws Exception {
        // when & then
        mockMvc.perform(get("/test/general-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("GLOBAL_5000"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.body").isEmpty());
    }
}
