package io.resume.make.domain.user.dto;

public record KakaoTokenResponse(
    String accessToken,
    String tokenType,
    String refreshToken,
    Long expiresIn,
    Long refreshTokenExpiresIn,
    String scope
) { }
