package io.resume.make.domain.user.repository;

import io.resume.make.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByKakaoId(String kakaoId);
    boolean existsByKakaoId(String kakaoId);
}
