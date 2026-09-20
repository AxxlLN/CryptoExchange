package com.crypto.exchange.integration.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class CoinLoreClientConfig {

    @Bean
    public RestClient.Builder coinLoreRestClientBuilder(CoinLoreProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .withReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .requestFactory(ClientHttpRequestFactories.get(SimpleClientHttpRequestFactory.class, settings));
    }
}