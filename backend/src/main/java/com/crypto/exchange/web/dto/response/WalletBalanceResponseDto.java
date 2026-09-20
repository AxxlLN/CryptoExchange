package com.crypto.exchange.web.dto.response;

import java.math.BigDecimal;

public record WalletBalanceResponseDto(
        Long id,
        Long cryptoId,
        String externalId,
        String symbol,
        String cryptoName,
        BigDecimal amount,
        BigDecimal currentPriceUsd,
        BigDecimal totalValueUsd
) {}