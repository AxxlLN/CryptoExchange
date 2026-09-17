package com.crypto.exchange.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponseDto(
        String senderAddress,
        String recipientAddress,
        String cryptoSymbol,
        BigDecimal amount,
        LocalDateTime timestamp,
        String status
) {}