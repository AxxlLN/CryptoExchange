package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.core.entity.WalletBalance;
import com.crypto.exchange.web.dto.response.WalletBalanceResponseDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WalletMapperTest {

    private final WalletMapper walletMapper = Mappers.getMapper(WalletMapper.class);

    private CryptoCurrency btcCurrency;
    private WalletBalance btcBalance;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        btcCurrency = CryptoCurrency.builder()
                .id(100L)
                .externalId("bitcoin")
                .symbol("BTC")
                .name("Bitcoin")
                .priceUsd(new BigDecimal("50000.00"))
                .build();

        btcBalance = WalletBalance.builder()
                .id(50L)
                .cryptoCurrency(btcCurrency)
                .amount(new BigDecimal("2.5"))
                .build();

        wallet = Wallet.builder()
                .id(10L)
                .address("0x123456789abcdef")
                .name("Main Wallet")
                .isDefault(true)
                .balances(Set.of(btcBalance))
                .build();

        btcBalance.setWallet(wallet);
    }

    @Nested
    @DisplayName("Маппинг Wallet -> WalletResponseDto")
    class WalletToDtoTests {

        @Test
        @DisplayName("Успешно маппит Wallet со всем содержимым и балансами")
        void toResponseDto_Success() {
            WalletResponseDto responseDto = walletMapper.toResponseDto(wallet);

            assertThat(responseDto).isNotNull();
            assertThat(responseDto.id()).isEqualTo(10L);
            assertThat(responseDto.address()).isEqualTo("0x123456789abcdef");
            assertThat(responseDto.name()).isEqualTo("Main Wallet");
            assertThat(responseDto.isDefault()).isTrue();
            assertThat(responseDto.balances()).hasSize(1);

            WalletBalanceResponseDto balanceDto = responseDto.balances().get(0);
            assertThat(balanceDto.id()).isEqualTo(50L);
            assertThat(balanceDto.cryptoId()).isEqualTo(100L);
            assertThat(balanceDto.externalId()).isEqualTo("bitcoin");
            assertThat(balanceDto.symbol()).isEqualTo("BTC");
            assertThat(balanceDto.cryptoName()).isEqualTo("Bitcoin");
            assertThat(balanceDto.amount()).isEqualByComparingTo("2.5");
            assertThat(balanceDto.currentPriceUsd()).isEqualByComparingTo("50000.00");
            assertThat(balanceDto.totalValueUsd()).isEqualByComparingTo("125000.0000");
        }

        @Test
        @DisplayName("Возвращает null при передаче null сущности Wallet")
        void toResponseDto_NullEntity_ReturnsNull() {
            assertThat(walletMapper.toResponseDto(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Маппинг WalletBalance -> WalletBalanceResponseDto и вспомогательные методы")
    class WalletBalanceToDtoTests {

        @Test
        @DisplayName("Успешный маппинг WalletBalance с корректным рассчетом стоимости")
        void toBalanceResponseDto_Success() {
            WalletBalanceResponseDto dto = walletMapper.toBalanceResponseDto(btcBalance);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(50L);
            assertThat(dto.cryptoId()).isEqualTo(100L);
            assertThat(dto.externalId()).isEqualTo("bitcoin");
            assertThat(dto.symbol()).isEqualTo("BTC");
            assertThat(dto.cryptoName()).isEqualTo("Bitcoin");
            assertThat(dto.amount()).isEqualByComparingTo("2.5");
            assertThat(dto.currentPriceUsd()).isEqualByComparingTo("50000.00");
            assertThat(dto.totalValueUsd()).isEqualByComparingTo("125000.0000");
        }

        @Test
        @DisplayName("Безопасная обработка null при вызове getCurrentPriceSafe и calculateTotalValue")
        void helperMethods_NullSafety() {
            assertThat(walletMapper.getCurrentPriceSafe(null)).isEqualTo(BigDecimal.ZERO);
            assertThat(walletMapper.calculateTotalValue(null)).isEqualTo(BigDecimal.ZERO);

            WalletBalance balanceWithoutCrypto = WalletBalance.builder().amount(new BigDecimal("10")).build();
            assertThat(walletMapper.getCurrentPriceSafe(balanceWithoutCrypto)).isEqualTo(BigDecimal.ZERO);
            assertThat(walletMapper.calculateTotalValue(balanceWithoutCrypto)).isEqualTo(BigDecimal.ZERO);

            CryptoCurrency cryptoWithNullPrice = CryptoCurrency.builder().priceUsd(null).build();
            WalletBalance balanceWithNullPrice = WalletBalance.builder()
                    .cryptoCurrency(cryptoWithNullPrice)
                    .amount(new BigDecimal("10"))
                    .build();

            assertThat(walletMapper.getCurrentPriceSafe(balanceWithNullPrice)).isEqualTo(BigDecimal.ZERO);
            assertThat(walletMapper.calculateTotalValue(balanceWithNullPrice)).isEqualTo(BigDecimal.ZERO);
        }
    }
}