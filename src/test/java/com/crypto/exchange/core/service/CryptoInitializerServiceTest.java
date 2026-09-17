package com.crypto.exchange.core.service;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.CryptoPriceHistoryRepository;
import com.crypto.exchange.integration.client.CoinLoreApiClient;
import com.crypto.exchange.integration.dto.CoinLoreResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoInitializerServiceTest {

    @Mock
    private CryptoCurrencyRepository cryptoCurrencyRepository;

    @Mock
    private CryptoPriceHistoryRepository cryptoPriceHistoryRepository;

    @Mock
    private CoinLoreApiClient coinLoreApiClient;

    @InjectMocks
    private CryptoInitializerService cryptoInitializerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cryptoInitializerService, "targetSymbols", Set.of("BTC"));
    }

    @Test
    void syncPrices_shouldFetchProcessAndSaveSuccessfully() {
        CoinLoreResponseDto.CoinLoreCryptoData apiData = new CoinLoreResponseDto.CoinLoreCryptoData(
                "90", "BTC", "Bitcoin", "65000.50"
        );
        CoinLoreResponseDto responseDto = new CoinLoreResponseDto(List.of(apiData));

        when(coinLoreApiClient.fetchLatestCryptoPrices()).thenReturn(Optional.of(responseDto));
        when(cryptoCurrencyRepository.findAllByExternalIdIn(anyCollection())).thenReturn(Collections.emptyList());
        when(cryptoCurrencyRepository.findAllBySymbolIn(anyCollection())).thenReturn(Collections.emptyList());

        when(cryptoCurrencyRepository.saveAll(anyCollection())).thenAnswer(invocation -> {
            List<CryptoCurrency> list = invocation.getArgument(0);
            list.forEach(c -> ReflectionTestUtils.setField(c, "id", 1L));
            return list;
        });

        cryptoInitializerService.syncPrices();

        verify(coinLoreApiClient, times(1)).fetchLatestCryptoPrices();

        ArgumentCaptor<List<CryptoCurrency>> cryptoCaptor = ArgumentCaptor.forClass(List.class);
        verify(cryptoCurrencyRepository).saveAll(cryptoCaptor.capture());
        List<CryptoCurrency> savedCryptos = cryptoCaptor.getValue();

        assertThat(savedCryptos).hasSize(1);
        assertThat(savedCryptos.get(0).getSymbol()).isEqualTo("BTC");
        assertThat(savedCryptos.get(0).getExternalId()).isEqualTo("90");
        assertThat(savedCryptos.get(0).getPriceUsd()).isEqualByComparingTo(new BigDecimal("65000.5000"));

        verify(cryptoPriceHistoryRepository, times(1)).saveAll(anyCollection());
    }

    @Test
    void syncPrices_shouldSkipWhenApiReturnsEmpty() {
        when(coinLoreApiClient.fetchLatestCryptoPrices()).thenReturn(Optional.empty());

        cryptoInitializerService.syncPrices();

        verify(coinLoreApiClient, times(1)).fetchLatestCryptoPrices();
        verifyNoInteractions(cryptoCurrencyRepository);
        verifyNoInteractions(cryptoPriceHistoryRepository);
    }

    @Test
    void syncPrices_shouldFilterOutSymbolsNotInTargetSet() {
        CoinLoreResponseDto.CoinLoreCryptoData apiData = new CoinLoreResponseDto.CoinLoreCryptoData(
                "80", "ETH", "Ethereum", "3000.00"
        );
        CoinLoreResponseDto responseDto = new CoinLoreResponseDto(List.of(apiData));

        when(coinLoreApiClient.fetchLatestCryptoPrices()).thenReturn(Optional.of(responseDto));

        cryptoInitializerService.syncPrices();

        verify(cryptoCurrencyRepository, never()).saveAll(anyCollection());
        verify(cryptoPriceHistoryRepository, never()).saveAll(anyCollection());
    }
}