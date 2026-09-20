package com.crypto.exchange.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record UpdateAvatarRequest(
        @NotBlank(message = "Avatar URL is required")
        @URL(message = "Invalid URL format")
        String avatarUrl
) {}