package com.crypto.exchange.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWalletRequestDto(
        @NotBlank(message = "Wallet name cannot be empty")
        @Size(max = 50, message = "Wallet name length must be <= 50 characters")
        String name,

        Boolean isDefault
) {}