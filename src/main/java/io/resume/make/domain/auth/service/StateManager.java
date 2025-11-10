package io.resume.make.domain.auth.service;

import io.resume.make.domain.auth.exception.OAuthErrorCode;
import io.resume.make.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StateManager {
    private static final String STATE_PREFIX = "oauth:state:";
    private static final Duration STATE_EXPIRATION = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * State 생성 및 저장
     * @param codeChallenge
     * @return 생성된 state
     */
    public String generateAndStoreState(String codeChallenge) {
        String state = UUID.randomUUID().toString();
        String key = STATE_PREFIX + state;

        redisTemplate.opsForValue().set(key, codeChallenge, STATE_EXPIRATION);
        log.debug("State generated: {}, stored in redis", state);

        return state;
    }

    public String validateAndConsumeState(String state) {
        String key = STATE_PREFIX + state;
        String codeChallenge = redisTemplate.opsForValue().get(key);
        if (codeChallenge == null) {
            log.error("State not found in redis: {}", state);
            throw new BusinessException(OAuthErrorCode.INVALID_STATE);
        }

        // 사용된 state 삭제
        redisTemplate.delete(key);
        log.debug("State consumed: {}", state);
        return codeChallenge;
    }



}
