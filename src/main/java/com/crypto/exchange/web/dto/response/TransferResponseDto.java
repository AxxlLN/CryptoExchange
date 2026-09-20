package com.crypto.exchange.web.dto.response;

import com.crypto.exchange.core.entity.Transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransferResponseDto(
        Long transactionId,
        String senderAddress,
        String recipientAddress,
        String cryptoSymbol,
        BigDecimal amount,
        OffsetDateTime timestamp,
        Transaction.TransactionStatus status
) {}