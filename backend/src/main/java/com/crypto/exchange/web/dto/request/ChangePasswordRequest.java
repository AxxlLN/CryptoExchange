package com.crypto.exchange.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Текущий пароль обязателен")
        String currentPassword,

        @NotBlank(message = "Новый пароль обязателен")
        @Size(min = 8, max = 100, message = "Новый пароль должен содержать от 8 до 100 символов")
        String newPassword
) {}