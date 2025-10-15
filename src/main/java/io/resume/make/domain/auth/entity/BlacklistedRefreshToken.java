package io.resume.make.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "backlisted_refresh_token",
        indexes = {@Index(name = "idx_user_id", columnList = "user_id")})
public class BlacklistedRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "Binary(16)")
    private UUID userId;

    private String refreshToken;

    private LocalDateTime createdAt;

    private LocalDateTime expiredAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.expiredAt = this.createdAt.plusDays(30);
    }
}
