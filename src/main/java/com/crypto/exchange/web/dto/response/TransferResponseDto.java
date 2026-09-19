package com.crypto.exchange.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransferResponseDto(
        String senderAddress,
        String recipientAddress,
        String cryptoSymbol,
        BigDecimal amount,
        OffsetDateTime timestamp,
        String status
) {}