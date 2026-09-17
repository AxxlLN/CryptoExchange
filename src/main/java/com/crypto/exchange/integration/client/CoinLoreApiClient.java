package com.crypto.exchange.integration.client;

import com.crypto.exchange.integration.config.CoinLoreProperties;
import com.crypto.exchange.integration.dto.CoinLoreResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Компонент-клиент для вызова стороннего CoinLore API через Spring RestClient.
 */
@Component
@Slf4j
public class CoinLoreApiClient {

    private final RestClient restClient;

    public CoinLoreApiClient(RestClient.Builder restClientBuilder, CoinLoreProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMs());
        requestFactory.setReadTimeout(properties.readTimeoutMs());

        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .requestFactory(requestFactory)
                .build();
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