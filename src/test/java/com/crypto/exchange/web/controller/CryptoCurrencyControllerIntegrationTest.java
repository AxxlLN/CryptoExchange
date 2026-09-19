package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.service.CryptoPriceHistoryService;
import com.crypto.exchange.core.service.CryptoService;
import com.crypto.exchange.web.dto.response.CryptoCurrencyResponseDto;
import com.crypto.exchange.web.dto.response.CryptoPriceHistoryDto;
import com.crypto.exchange.web.security.JwtTokenProvider;
import com.crypto.exchange.web.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    void getAllCryptos_shouldReturnList_whenCryptosExist() throws Exception {
        CryptoCurrencyResponseDto dto = new CryptoCurrencyResponseDto(
                1L, "BTC", "Bitcoin", new BigDecimal("65000.00"), OffsetDateTime.now(ZoneOffset.UTC)
        );

        given(cryptoService.getAllCryptocurrencies()).willReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/cryptos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[0].name").value("Bitcoin"))
                .andExpect(jsonPath("$[0].priceUsd").value(65000.00));
    }

    @Test
    void getCryptoById_shouldReturnCrypto_whenIdExists() throws Exception {
        Long cryptoId = 1L;
        CryptoCurrencyResponseDto dto = new CryptoCurrencyResponseDto(
                cryptoId, "ETH", "Ethereum", new BigDecimal("3500.50"), OffsetDateTime.now(ZoneOffset.UTC)
        );

        given(cryptoService.getCryptoById(cryptoId)).willReturn(dto);

        mockMvc.perform(get("/api/v1/cryptos/{id}", cryptoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cryptoId))
                .andExpect(jsonPath("$.symbol").value("ETH"))
                .andExpect(jsonPath("$.name").value("Ethereum"));
    }

    @Test
    void getPriceHistory_shouldReturnPageOfHistory_whenValidIdProvided() throws Exception {
        Long cryptoId = 1L;
        CryptoPriceHistoryDto historyDto = new CryptoPriceHistoryDto(
                new BigDecimal("3450.00"), OffsetDateTime.now(ZoneOffset.UTC)
        );

        Page<CryptoPriceHistoryDto> historyPage = new PageImpl<>(List.of(historyDto));

        given(priceHistoryService.getPriceHistory(eq(cryptoId), any(Pageable.class))).willReturn(historyPage);

        mockMvc.perform(get("/api/v1/cryptos/{id}/history", cryptoId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].priceUsd").value(3450.00))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}