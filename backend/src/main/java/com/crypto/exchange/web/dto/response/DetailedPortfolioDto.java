package com.crypto.exchange.web.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DetailedPortfolioDto(
        BigDecimal totalBalanceUsd,
        List<CryptoAssetBalanceDto> assets,
        PortfolioAnalyticsDto analytics
) {
    public record CryptoAssetBalanceDto(
            Long cryptoId,
            String symbol,
            String name,
            BigDecimal totalAmount,
            BigDecimal currentPriceUsd,
            BigDecimal totalValueUsd,
            BigDecimal percentageOfPortfolio
    ) {}

    public record PortfolioAnalyticsDto(
            BigDecimal pnl24hUsd,
            BigDecimal pnl24hPercentage,
            BigDecimal pnl7dUsd,
            BigDecimal pnl7dPercentage
    ) {}
}