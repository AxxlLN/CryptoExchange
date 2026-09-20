package com.crypto.exchange.web.dto.response;

import com.crypto.exchange.core.entity.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record UserResponseDto(
        Long id,
        String username,
        String email,
        String avatarUrl,
        Role role,

        @JsonProperty("isBlocked")
        boolean blocked,

        @JsonProperty("isDeleted")
        boolean deleted,

        OffsetDateTime createdAt
) {}