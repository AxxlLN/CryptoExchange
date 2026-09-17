package com.crypto.exchange.web.dto.request;

import com.crypto.exchange.core.entity.Transaction.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionRequestDto(
        Long id,
        Long fromWalletId,
        Long toWalletId,
        String fromCryptoSymbol,
        String toCryptoSymbol,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        TransactionType type,
        OffsetDateTime createdAt
) {}
