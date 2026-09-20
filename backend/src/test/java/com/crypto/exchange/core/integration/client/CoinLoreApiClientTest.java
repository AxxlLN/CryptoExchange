package com.crypto.exchange.core.integration.client;

import com.crypto.exchange.integration.client.CoinLoreApiClient;
import com.crypto.exchange.integration.config.CoinLoreProperties;
import com.crypto.exchange.integration.dto.CoinLoreResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

@RestClientTest(CoinLoreApiClient.class)
@EnableConfigurationProperties(CoinLoreProperties.class)
class CoinLoreApiClientTest {

    @Autowired
    private CoinLoreApiClient apiClient;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    @DisplayName("fetchLatestCryptoPrices успешно возвращает данные при ответе 200 OK")
    void fetchLatestCryptoPricesSuccess() {
        String jsonResponse = """
                {
                  "data": [
                    {
                      "id": "90",
                      "symbol": "BTC",
                      "name": "Bitcoin",
                      "price_usd": "65000.50"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("?start=0&limit=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        Optional<CoinLoreResponseDto> result = apiClient.fetchLatestCryptoPrices();

        assertThat(result).isPresent();
        assertThat(result.get().data()).hasSize(1);
        assertThat(result.get().data().get(0).symbol()).isEqualTo("BTC");
        mockServer.verify();
    }

    @Test
    @DisplayName("fetchLatestCryptoPrices возвращает Optional.empty() и перехватывает ошибку при ответе 500 Server Error")
    void fetchLatestCryptoPricesServerErrorReturnsEmpty() {
        mockServer.expect(requestTo("?start=0&limit=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        Optional<CoinLoreResponseDto> result = apiClient.fetchLatestCryptoPrices();

        assertThat(result).isEmpty();
        mockServer.verify();
    }
}