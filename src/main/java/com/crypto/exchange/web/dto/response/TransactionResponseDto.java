package com.crypto.exchange.web.dto.response;

import com.crypto.exchange.core.entity.Transaction.TransactionStatus;
import com.crypto.exchange.core.entity.Transaction.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponseDto(
        Long id,
        String fromWalletAddress,
        String toWalletAddress,
        String fromCryptoSymbol,
        String toCryptoSymbol,
        BigDecimal fromAmount,
        BigDecimal toAmount,
        TransactionType type,
        TransactionStatus status,
        TransactionDirection direction,
        OffsetDateTime createdAt
) {
    public enum TransactionDirection {
        INCOMING,
        OUTGOING,
        SELF
    }
}