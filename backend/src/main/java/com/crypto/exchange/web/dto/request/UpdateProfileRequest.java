package com.crypto.exchange.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Имя пользователя не может быть пустым")
        @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
        String username
) {}