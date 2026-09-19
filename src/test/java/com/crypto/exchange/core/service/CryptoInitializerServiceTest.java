package com.crypto.exchange.core.service;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.CryptoPriceHistoryRepository;
import com.crypto.exchange.integration.client.CoinLoreApiClient;
import com.crypto.exchange.integration.dto.CoinLoreResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

    @Captor
    private ArgumentCaptor<List<CryptoCurrency>> cryptoListCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cryptoInitializerService, "targetSymbols", Set.of("BTC"));
    }

    @Test
    @DisplayName("syncPrices успешно создает новую криптовалюту, если её нет в БД")
    void syncPricesShouldFetchProcessAndSaveSuccessfully() {
        mockApiSuccess("90", "BTC", "Bitcoin", "65000.50");
        when(cryptoCurrencyRepository.findAllByExternalIdIn(anyCollection())).thenReturn(Collections.emptyList());
        when(cryptoCurrencyRepository.findAllBySymbolIn(anyCollection())).thenReturn(Collections.emptyList());

        when(cryptoCurrencyRepository.saveAll(anyCollection())).thenAnswer(invocation -> {
            List<CryptoCurrency> list = invocation.getArgument(0);
            list.forEach(c -> ReflectionTestUtils.setField(c, "id", 1L));
            return list;
        });

        cryptoInitializerService.syncPrices();

        CryptoCurrency saved = captureSingleSavedCrypto();
        assertThat(saved.getSymbol()).isEqualTo("BTC");
        assertThat(saved.getExternalId()).isEqualTo("90");
        assertThat(saved.getPriceUsd()).isEqualByComparingTo(new BigDecimal("65000.5000"));

        verify(cryptoPriceHistoryRepository, times(1)).saveAll(anyCollection());
    }

    @Test
    @DisplayName("syncPrices не выполняет действий, если API вернул empty")
    void syncPricesShouldSkipWhenApiReturnsEmpty() {
        when(coinLoreApiClient.fetchLatestCryptoPrices()).thenReturn(Optional.empty());

        cryptoInitializerService.syncPrices();

        verify(coinLoreApiClient, times(1)).fetchLatestCryptoPrices();
        verifyNoInteractions(cryptoCurrencyRepository, cryptoPriceHistoryRepository);
    }

    @Test
    @DisplayName("syncPrices фильтрует монеты, отсутствующие в targetSymbols")
    void syncPricesShouldFilterOutSymbolsNotInTargetSet() {
        mockApiSuccess("80", "ETH", "Ethereum", "3000.00");

        cryptoInitializerService.syncPrices();

        verify(cryptoCurrencyRepository, never()).saveAll(anyCollection());
        verify(cryptoPriceHistoryRepository, never()).saveAll(anyCollection());
    }

    @Test
    @DisplayName("syncPrices обновляет запись, если сущность найдена по externalId")
    void syncPricesShouldUpdateExistingCryptoFoundByExternalId() {
        mockApiSuccess("90", "BTC", "Bitcoin", "70000.00");
        CryptoCurrency existingCrypto = createCrypto(1L, "90", "BTC", "Bitcoin", "60000.0000");

        when(cryptoCurrencyRepository.findAllByExternalIdIn(anyCollection())).thenReturn(List.of(existingCrypto));
        when(cryptoCurrencyRepository.findAllBySymbolIn(anyCollection())).thenReturn(Collections.emptyList());
        when(cryptoCurrencyRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        cryptoInitializerService.syncPrices();

        CryptoCurrency saved = captureSingleSavedCrypto();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getExternalId()).isEqualTo("90");
        assertThat(saved.getPriceUsd()).isEqualByComparingTo(new BigDecimal("70000.0000"));

        verify(cryptoPriceHistoryRepository, times(1)).saveAll(anyCollection());
    }

    @Test
    @DisplayName("syncPrices обновляет запись и присваивает externalId, если сущность найдена только по symbol")
    void syncPricesShouldUpdateExistingCryptoFoundBySymbolWhenExternalIdNotFound() {
        mockApiSuccess("90", "BTC", "Bitcoin", "72000.00");
        CryptoCurrency existingCrypto = createCrypto(2L, null, "BTC", "Bitcoin", "65000.0000");

        when(cryptoCurrencyRepository.findAllByExternalIdIn(anyCollection())).thenReturn(Collections.emptyList());
        when(cryptoCurrencyRepository.findAllBySymbolIn(anyCollection())).thenReturn(List.of(existingCrypto));
        when(cryptoCurrencyRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        cryptoInitializerService.syncPrices();

        CryptoCurrency saved = captureSingleSavedCrypto();
        assertThat(saved.getId()).isEqualTo(2L);
        assertThat(saved.getExternalId()).isEqualTo("90");
        assertThat(saved.getPriceUsd()).isEqualByComparingTo(new BigDecimal("72000.0000"));

        verify(cryptoPriceHistoryRepository, times(1)).saveAll(anyCollection());
    }

    private void mockApiSuccess(String id, String symbol, String name, String price) {
        CoinLoreResponseDto.CoinLoreCryptoData apiData =
                new CoinLoreResponseDto.CoinLoreCryptoData(id, symbol, name, price);
        CoinLoreResponseDto responseDto = new CoinLoreResponseDto(List.of(apiData));
        when(coinLoreApiClient.fetchLatestCryptoPrices()).thenReturn(Optional.of(responseDto));
    }

    private CryptoCurrency createCrypto(Long id, String externalId, String symbol, String name, String priceUsd) {
        return CryptoCurrency.builder()
                .id(id)
                .externalId(externalId)
                .symbol(symbol)
                .name(name)
                .priceUsd(new BigDecimal(priceUsd))
                .build();
    }

    private CryptoCurrency captureSingleSavedCrypto() {
        verify(cryptoCurrencyRepository).saveAll(cryptoListCaptor.capture());
        List<CryptoCurrency> savedList = cryptoListCaptor.getValue();
        assertThat(savedList).hasSize(1);
        return savedList.get(0);
    }
}