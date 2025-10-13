package io.resume.make.domain.auth.dto;

public record KakaoTokenResponse(
    String accessToken,
    String tokenType,
    String refreshToken,
    Long expiresIn,
    Long refreshTokenExpiresIn,
    String scope
) { }
