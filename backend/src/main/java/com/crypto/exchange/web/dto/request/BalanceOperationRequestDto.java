package com.crypto.exchange.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BalanceOperationRequestDto(
        @NotBlank(message = "External currency ID is required")
        String externalId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00000001", message = "Amount must be greater than 0")
        BigDecimal amount
) {}