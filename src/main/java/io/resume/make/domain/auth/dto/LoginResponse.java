package io.resume.make.domain.auth.dto;

import io.resume.make.domain.user.entity.User;
import lombok.Builder;

import java.util.UUID;

@Builder
public record LoginResponse(
        String accessToken,
        UUID userId,
        String email,
        String nickname,
        String refreshToken
) {
    public static LoginResponse of(User user, String jwtAccessToken, String jwtRefreshToken) {
        return LoginResponse.builder()
                .accessToken(jwtAccessToken)
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getName())
                .refreshToken(jwtRefreshToken)
                .build();
    }
}

