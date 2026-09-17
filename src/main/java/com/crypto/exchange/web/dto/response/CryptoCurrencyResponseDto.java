package com.crypto.exchange.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CryptoCurrencyResponseDto(
        Long id,
        String symbol,
        String name,
        BigDecimal priceUsd,
        OffsetDateTime updatedAt
) {}