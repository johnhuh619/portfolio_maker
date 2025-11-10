package io.resume.make.domain.auth.repository;

import io.resume.make.domain.auth.entity.BlacklistedRefreshToken;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedRefreshToken, Long> {
    boolean existsByTokenHash(byte[] tokenHash);

    @Modifying
    @Transactional
    void deleteAllByExpiredAtBefore(LocalDateTime now);
}
