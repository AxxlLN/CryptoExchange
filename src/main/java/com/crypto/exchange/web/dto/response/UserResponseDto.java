package com.crypto.exchange.web.dto.response;

import com.crypto.exchange.core.entity.Role;

import java.time.OffsetDateTime;

public record UserResponseDto(
        Long id,
        String username,
        String email,
        Role role,
        OffsetDateTime createdAt
) {}