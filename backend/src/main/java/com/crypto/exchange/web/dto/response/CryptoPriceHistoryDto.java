package com.crypto.exchange.web.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO для передачи элементов истории стоимости пользователям нашего REST API.
 */
public record CryptoPriceHistoryDto(
        BigDecimal priceUsd,
        OffsetDateTime recordedAt
) {}