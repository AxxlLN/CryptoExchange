package com.crypto.exchange.web.dto.response;

import java.math.BigDecimal;

public record AggregatedBalanceDto(
        Long cryptoId,
        String symbol,
        String name,
        BigDecimal totalAmount
) {}
