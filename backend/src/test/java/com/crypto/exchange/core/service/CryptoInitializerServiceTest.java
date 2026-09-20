package com.crypto.exchange.core.service;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.CryptoPriceHistory;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.CryptoPriceHistoryRepository;
import com.crypto.exchange.integration.client.CoinLoreApiClient;
import com.crypto.exchange.integration.dto.CoinLoreResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoInitializerServiceTest {

    @Mock
    private CryptoCurrencyRepository cryptoRepository;

    @Mock
    private CryptoPriceHistoryRepository priceHistoryRepository;

    @Mock
    private CoinLoreApiClient coinLoreApiClient;

    @InjectMocks
    private CryptoInitializerService cryptoInitializerService;

    private Set<String> targetSymbols;

    @BeforeEach
    void setUp() {
        targetSymbols = Set.of("BTC", "ETH", "USDT");
        ReflectionTestUtils.setField(cryptoInitializerService, "targetSymbols", targetSymbols);
    }

    @Nested
    @DisplayName("Тесты методов scheduledPriceUpdate и syncPrices")
    class SyncPricesTests {

        @Test
        void syncPricesShouldDoNothingWhenApiReturnsEmptyOptional() {
            when(coinLoreApiClient.fetchLatestCryptoPrices()).thenReturn(Optional.empty());

            cryptoInitializerService.syncPrices();

            verify(cryptoRepository, never()).saveAll(any());
            verify(priceHistoryRepository, never()).saveAll(any());
        }

        @Test
        void scheduledPriceUpdateShouldTriggerSyncPrices() {
            when(coinLoreApiClient.fetchLatestCryptoPrices()).thenReturn(Optional.empty());

            cryptoInitializerService.scheduledPriceUpdate();

            verify(coinLoreApiClient).fetchLatestCryptoPrices();
        }
    }

    @Nested
    @DisplayName("Тесты обработки и сохранения данных (processAndSaveBatch)")
    class ProcessAndSaveBatchTests {

        @Test
        void processAndSaveBatchShouldReturnEarlyWhenNoTargetSymbolsMatch() {
            var nonTargetData = new CoinLoreResponseDto.CoinLoreCryptoData("999", "DOGE", "Dogecoin", "0.10");

            cryptoInitializerService.processAndSaveBatch(List.of(nonTargetData));

            verify(cryptoRepository, never()).findAllByExternalIdIn(any());
            verify(cryptoRepository, never()).saveAll(any());
            verify(priceHistoryRepository, never()).saveAll(any());
        }

        @Test
        void processAndSaveBatchShouldCreateNewCryptoWhenNotExists() {
            var btcApiData = new CoinLoreResponseDto.CoinLoreCryptoData("90", "BTC", "Bitcoin", "65000.12345");

            when(cryptoRepository.findAllByExternalIdIn(Set.of("90"))).thenReturn(Collections.emptyList());
            when(cryptoRepository.findAllBySymbolIn(Set.of("BTC"))).thenReturn(Collections.emptyList());

            CryptoCurrency savedBtc = CryptoCurrency.builder()
                    .id(1L)
                    .externalId("90")
                    .symbol("BTC")
                    .name("Bitcoin")
                    .priceUsd(new BigDecimal("65000.1235"))
                    .build();

            when(cryptoRepository.saveAll(any())).thenReturn(List.of(savedBtc));

            cryptoInitializerService.processAndSaveBatch(List.of(btcApiData));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<CryptoCurrency>> cryptoCaptor = ArgumentCaptor.forClass(List.class);
            verify(cryptoRepository).saveAll(cryptoCaptor.capture());

            List<CryptoCurrency> savedList = cryptoCaptor.getValue();
            assertThat(savedList).hasSize(1);
            CryptoCurrency newCrypto = savedList.get(0);

            assertThat(newCrypto.getExternalId()).isEqualTo("90");
            assertThat(newCrypto.getSymbol()).isEqualTo("BTC");
            assertThat(newCrypto.getName()).isEqualTo("Bitcoin");
            assertThat(newCrypto.getPriceUsd()).isEqualTo(new BigDecimal("65000.1235"));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<CryptoPriceHistory>> historyCaptor = ArgumentCaptor.forClass(List.class);
            verify(priceHistoryRepository).saveAll(historyCaptor.capture());

            List<CryptoPriceHistory> historyList = historyCaptor.getValue();
            assertThat(historyList).hasSize(1);
            assertThat(historyList.get(0).getCryptoCurrency()).isEqualTo(savedBtc);
            assertThat(historyList.get(0).getPriceUsd()).isEqualTo(new BigDecimal("65000.1235"));
        }

        @Test
        void processAndSaveBatchShouldUpdateExistingCryptoByExternalId() {
            var ethApiData = new CoinLoreResponseDto.CoinLoreCryptoData("80", "ETH", "Ethereum", "3500.00");

            CryptoCurrency existingEth = CryptoCurrency.builder()
                    .id(2L)
                    .externalId("80")
                    .symbol("ETH")
                    .name("Ethereum")
                    .priceUsd(new BigDecimal("3000.00"))
                    .build();

            when(cryptoRepository.findAllByExternalIdIn(Set.of("80"))).thenReturn(List.of(existingEth));
            when(cryptoRepository.findAllBySymbolIn(Set.of("ETH"))).thenReturn(List.of(existingEth));

            when(cryptoRepository.saveAll(any())).thenReturn(List.of(existingEth));

            cryptoInitializerService.processAndSaveBatch(List.of(ethApiData));

            assertThat(existingEth.getPriceUsd()).isEqualTo(new BigDecimal("3500.0000").setScale(4, RoundingMode.HALF_UP));

            verify(cryptoRepository).saveAll(List.of(existingEth));
            verify(priceHistoryRepository).saveAll(any());
        }

        @Test
        void processAndSaveBatchShouldUpdateExistingCryptoBySymbolWhenExternalIdNotFound() {
            var usdtApiData = new CoinLoreResponseDto.CoinLoreCryptoData("518", "USDT", "Tether", "1.00");

            CryptoCurrency existingUsdtWithoutExternalId = CryptoCurrency.builder()
                    .id(3L)
                    .externalId(null)
                    .symbol("USDT")
                    .name("Tether")
                    .priceUsd(new BigDecimal("0.99"))
                    .build();

            when(cryptoRepository.findAllByExternalIdIn(Set.of("518"))).thenReturn(Collections.emptyList());
            when(cryptoRepository.findAllBySymbolIn(Set.of("USDT"))).thenReturn(List.of(existingUsdtWithoutExternalId));

            when(cryptoRepository.saveAll(any())).thenReturn(List.of(existingUsdtWithoutExternalId));

            cryptoInitializerService.processAndSaveBatch(List.of(usdtApiData));

            assertThat(existingUsdtWithoutExternalId.getExternalId()).isEqualTo("518");
            assertThat(existingUsdtWithoutExternalId.getPriceUsd()).isEqualTo(new BigDecimal("1.0000"));

            verify(cryptoRepository).saveAll(List.of(existingUsdtWithoutExternalId));
            verify(priceHistoryRepository).saveAll(any());
        }
    }
}