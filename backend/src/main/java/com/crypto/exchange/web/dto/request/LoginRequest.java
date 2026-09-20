package com.crypto.exchange.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Укажите username или email")
        String usernameOrEmail,

        @NotBlank(message = "Пароль не может быть пустым")
        String password
) {}