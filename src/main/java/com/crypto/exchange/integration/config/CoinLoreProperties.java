package com.crypto.exchange.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.coinlore")
public record CoinLoreProperties(
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs
) {}