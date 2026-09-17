package com.crypto.exchange.core.integration.client;

import com.crypto.exchange.integration.client.CoinLoreApiClient;
import com.crypto.exchange.integration.config.CoinLoreProperties;
import com.crypto.exchange.integration.dto.CoinLoreResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest({CoinLoreApiClient.class, CoinLoreProperties.class})
class CoinLoreApiClientTest {

    @Autowired
    private CoinLoreApiClient apiClient;

    @Autowired
    private MockRestServiceServer mockServer;
}