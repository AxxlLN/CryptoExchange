package com.crypto.exchange.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequestDto(
        @NotNull(message = "Укажите адрес кошелька списания")
        String fromAddress,

        @NotBlank(message = "Укажите адрес кошелька получателя")
        String recipientAddress,

        @NotBlank(message = "Укажите external_id криптовалюты (например, bitcoin, ethereum)")
        String cryptoExternalId,

        @NotNull(message = "Укажите сумму перевода")
        @DecimalMin(value = "0.00000001", message = "Сумма должна быть больше 0")
        BigDecimal amount
) {}