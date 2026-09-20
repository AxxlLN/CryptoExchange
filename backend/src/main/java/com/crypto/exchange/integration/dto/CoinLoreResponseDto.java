package com.crypto.exchange.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO ответа от CoinLore API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinLoreResponseDto(
        List<CoinLoreCryptoData> data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoinLoreCryptoData(
            String id,
            String symbol,
            String name,
            @JsonProperty("price_usd") String priceUsd
    ) {

    }
}