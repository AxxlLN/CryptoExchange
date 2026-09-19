package com.crypto.exchange.integration.client;

import com.crypto.exchange.integration.dto.CoinLoreResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
@Slf4j
public class CoinLoreApiClient {

    private final RestClient restClient;

    public CoinLoreApiClient(RestClient.Builder coinLoreRestClientBuilder) {
        this.restClient = coinLoreRestClientBuilder.build();
    }

    public Optional<CoinLoreResponseDto> fetchLatestCryptoPrices() {
        try {
            CoinLoreResponseDto response = restClient.get()
                    .uri("?start=0&limit=100")
                    .retrieve()
                    .body(CoinLoreResponseDto.class);

            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Crypto API Error: {}", e.getMessage());
            return Optional.empty();
        }
    }
}