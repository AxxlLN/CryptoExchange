package com.crypto.exchange.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExchangeRequest(
        @NotNull(message = "Укажите ID кошелька списания")
        Long fromWalletId,

        @NotNull(message = "Укажите ID кошелька зачисления")
        Long toWalletId,

        @NotNull(message = "Укажите ID исходной валюты")
        Long fromCryptoId,

        @NotNull(message = "Укажите ID целевой валюты")
        Long toCryptoId,

        @NotNull(message = "Укажите сумму для обмена")
        @DecimalMin(value = "0.00000001", message = "Сумма должна быть больше 0")
        BigDecimal amount
) {}