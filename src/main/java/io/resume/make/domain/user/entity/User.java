package io.resume.make.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users", indexes = {
                @Index(name = "idx_provider_id", columnList = "provider_id")
})
public class User {

    @Id
    @GeneratedValue
    @Column(name = "user_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(length = 100)
    private String provider;

    @Column(name = "provider_id", length = 100, nullable = false, unique = true)
    private String providerId;

    @Column(length = 255)
    private String email;

    @Column(length = 100)
    private String name;

//    @Column(name = "profile_image", length = 100)
//    private String profileImg;

    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Builder
    public User(String provider, String providerId, String email, String name) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
//        this.profileImg = profileImg;
    }

    public void updateName(String name) {
        if (!Objects.equals(this.name, name)){
            this.name = name;
        }
    }
}
