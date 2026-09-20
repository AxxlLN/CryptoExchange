package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.service.CryptoPriceHistoryService;
import com.crypto.exchange.core.service.CryptoService;
import com.crypto.exchange.web.dto.response.CryptoCurrencyResponseDto;
import com.crypto.exchange.web.dto.response.CryptoPriceHistoryDto;
import com.crypto.exchange.web.security.JwtTokenProvider;
import com.crypto.exchange.web.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CryptoCurrencyController.class)
@Import(SecurityConfig.class)
class CryptoCurrencyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CryptoService cryptoService;

    @MockBean
    private CryptoPriceHistoryService priceHistoryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    private CryptoCurrencyResponseDto createCryptoDto(Long id, String symbol, String name, BigDecimal priceUsd) {
        return new CryptoCurrencyResponseDto(
                id,
                symbol,
                name,
                priceUsd,
                OffsetDateTime.of(2026, 9, 20, 12, 0, 0, 0, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("GET /api/v1/cryptos - должен возвращать 200 OK и список криптовалют")
    void getAllCryptosShouldReturn200AndList() throws Exception {
        List<CryptoCurrencyResponseDto> cryptos = List.of(
                createCryptoDto(1L, "BTC", "Bitcoin", new BigDecimal("65000.00")),
                createCryptoDto(2L, "ETH", "Ethereum", new BigDecimal("3500.00"))
        );

        when(cryptoService.getAllCryptocurrencies()).thenReturn(cryptos);

        mockMvc.perform(get("/api/v1/cryptos")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[0].name").value("Bitcoin"))
                .andExpect(jsonPath("$[0].priceUsd").value(65000.00))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].symbol").value("ETH"))
                .andExpect(jsonPath("$[1].name").value("Ethereum"));

        verify(cryptoService, times(1)).getAllCryptocurrencies();
    }

    @Test
    @DisplayName("GET /api/v1/cryptos/{id} - должен возвращать 200 OK и данные криптовалюты")
    void getCryptoByIdShouldReturn200AndCrypto() throws Exception {
        Long cryptoId = 1L;
        CryptoCurrencyResponseDto cryptoDto = createCryptoDto(cryptoId, "BTC", "Bitcoin", new BigDecimal("65000.00"));

        when(cryptoService.getCryptoById(cryptoId)).thenReturn(cryptoDto);

        mockMvc.perform(get("/api/v1/cryptos/{id}", cryptoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.name").value("Bitcoin"))
                .andExpect(jsonPath("$.priceUsd").value(65000.00));

        verify(cryptoService, times(1)).getCryptoById(cryptoId);
    }

    @Test
    @DisplayName("GET /api/v1/cryptos/{id} - должен возвращать 404 Not Found, если криптовалюта не найдена")
    void getCryptoByIdShouldReturn404WhenNotFound() throws Exception {
        Long nonExistingId = 999L;

        when(cryptoService.getCryptoById(nonExistingId))
                .thenThrow(new ResourceNotFoundException("Cryptocurrency not found with id: " + nonExistingId));

        mockMvc.perform(get("/api/v1/cryptos/{id}", nonExistingId))
                .andExpect(status().isNotFound());

        verify(cryptoService, times(1)).getCryptoById(nonExistingId);
    }

    @Test
    @DisplayName("GET /api/v1/cryptos/{id}/history - должен возвращать 200 OK и страницу с историей цен")
    void getPriceHistoryShouldReturn200AndPage() throws Exception {
        Long cryptoId = 1L;
        CryptoPriceHistoryDto historyItem = new CryptoPriceHistoryDto(
                new BigDecimal("64500.00"),
                OffsetDateTime.of(2026, 9, 20, 11, 0, 0, 0, ZoneOffset.UTC)
        );

        Page<CryptoPriceHistoryDto> pageResponse = new PageImpl<>(
                List.of(historyItem),
                PageRequest.of(0, 20),
                1
        );

        when(priceHistoryService.getPriceHistory(eq(cryptoId), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/cryptos/{id}/history", cryptoId)
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].priceUsd").value(64500.00))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(priceHistoryService, times(1)).getPriceHistory(eq(cryptoId), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/v1/cryptos/{id}/history - должен возвращать 404 Not Found, если криптовалюта не найдена")
    void getPriceHistoryShouldReturn404WhenNotFound() throws Exception {
        Long nonExistingId = 999L;

        when(priceHistoryService.getPriceHistory(eq(nonExistingId), any(Pageable.class)))
                .thenThrow(new ResourceNotFoundException("Cryptocurrency not found with id: " + nonExistingId));

        mockMvc.perform(get("/api/v1/cryptos/{id}/history", nonExistingId))
                .andExpect(status().isNotFound());

        verify(priceHistoryService, times(1)).getPriceHistory(eq(nonExistingId), any(Pageable.class));
    }
}