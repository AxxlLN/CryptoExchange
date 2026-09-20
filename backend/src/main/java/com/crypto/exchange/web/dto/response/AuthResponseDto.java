package com.crypto.exchange.web.dto.response;

public record AuthResponseDto(
        String token,
        String tokenType,
        UserResponseDto user
) {
    public AuthResponseDto(String token, UserResponseDto user) {
        this(token, "Bearer", user);
    }
}