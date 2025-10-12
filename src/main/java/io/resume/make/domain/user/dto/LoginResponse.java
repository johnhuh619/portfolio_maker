package io.resume.make.domain.user.dto;

public record LoginResponse(
        String accessToken,
        Long userId,
        String email,
        String nickname,
        String refreshToken
) { }

