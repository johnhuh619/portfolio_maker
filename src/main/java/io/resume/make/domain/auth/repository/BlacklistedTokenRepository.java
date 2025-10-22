package io.resume.make.domain.auth.repository;

import io.resume.make.domain.auth.entity.BlacklistedRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedRefreshToken, Long> {
    boolean existsByRefreshToken(String refreshToken);
    void deleteAllByExpiredAtBefore(LocalDateTime now);
}
