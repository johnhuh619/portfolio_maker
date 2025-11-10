package io.resume.make.domain.auth.service;

import io.resume.make.domain.auth.exception.OAuthErrorCode;
import io.resume.make.global.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("StateManager 단위 테스트")
class StateManagerTest {

    @Autowired
    private StateManager stateManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void tearDown() {
        // Redis 데이터 정리
        redisTemplate.getConnectionFactory().getConnection().flushDb();
    }

    @Test
    @DisplayName("State 생성 및 Redis 저장 성공")
    void generateAndStoreState_Success() {
        // given
        String codeChallenge = "test-code-challenge-123";

        // when
        String state = stateManager.generateAndStoreState(codeChallenge);

        // then
        assertThat(state).isNotNull();
        assertThat(state).isNotEmpty();

        // Redis에 저장되었는지 확인
        String key = "oauth:state:" + state;
        String storedChallenge = redisTemplate.opsForValue().get(key);
        assertThat(storedChallenge).isEqualTo(codeChallenge);
    }

    @Test
    @DisplayName("State 검증 성공 - 저장된 codeChallenge 반환")
    void validateAndConsumeState_Success() {
        // given
        String codeChallenge = "valid-code-challenge";
        String state = stateManager.generateAndStoreState(codeChallenge);

        // when
        String retrievedChallenge = stateManager.validateAndConsumeState(state);

        // then
        assertThat(retrievedChallenge).isEqualTo(codeChallenge);

        // State는 소비되어 삭제되어야 함
        String key = "oauth:state:" + state;
        String deletedValue = redisTemplate.opsForValue().get(key);
        assertThat(deletedValue).isNull();
    }

    @Test
    @DisplayName("State 검증 실패 - 존재하지 않는 State")
    void validateAndConsumeState_InvalidState_ThrowsException() {
        // given
        String invalidState = "non-existent-state";

        // when & then
        assertThatThrownBy(() -> stateManager.validateAndConsumeState(invalidState))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OAuthErrorCode.INVALID_STATE);
    }

    @Test
    @DisplayName("State 재사용 방지 - 한번 사용한 State는 재사용 불가")
    void validateAndConsumeState_StateReuse_ThrowsException() {
        // given
        String codeChallenge = "test-challenge";
        String state = stateManager.generateAndStoreState(codeChallenge);

        // when - 첫 번째 사용 (성공)
        stateManager.validateAndConsumeState(state);

        // then - 두 번째 사용 (실패)
        assertThatThrownBy(() -> stateManager.validateAndConsumeState(state))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", OAuthErrorCode.INVALID_STATE);
    }

    @Test
    @DisplayName("각 State는 고유한 UUID여야 함")
    void generateAndStoreState_UniqueStates() {
        // given
        String codeChallenge1 = "challenge1";
        String codeChallenge2 = "challenge2";

        // when
        String state1 = stateManager.generateAndStoreState(codeChallenge1);
        String state2 = stateManager.generateAndStoreState(codeChallenge2);

        // then
        assertThat(state1).isNotEqualTo(state2);
    }
}
