package com.crypto.exchange.core.event;

import com.crypto.exchange.web.dto.response.TransactionResponseDto;

public record TransactionCompletedEvent(
        String recipientEmail,
        TransactionResponseDto transaction
) {}