package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.core.entity.WalletBalance;
import com.crypto.exchange.web.dto.response.WalletBalanceResponseDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WalletMapperTest {

    private final WalletMapper walletMapper = Mappers.getMapper(WalletMapper.class);

    @Test
    void toResponseDtoShouldMapAllFieldsCorrectly() {
        CryptoCurrency btc = CryptoCurrency.builder()
                .id(10L)
                .externalId("bitcoin")
                .symbol("BTC")
                .name("Bitcoin")
                .priceUsd(new BigDecimal("50000.00"))
                .build();

        WalletBalance balance = WalletBalance.builder()
                .id(1L)
                .cryptoCurrency(btc)
                .amount(new BigDecimal("2.0"))
                .build();

        Wallet wallet = Wallet.builder()
                .id(100L)
                .address("0x12345")
                .name("Main Wallet")
                .isDefault(true)
                .balances(Set.of(balance))
                .build();

        WalletResponseDto dto = walletMapper.toResponseDto(wallet);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.address()).isEqualTo("0x12345");
        assertThat(dto.name()).isEqualTo("Main Wallet");
        assertThat(dto.isDefault()).isTrue();
        assertThat(dto.balances()).hasSize(1);

        WalletBalanceResponseDto balanceDto = dto.balances().get(0);
        assertThat(balanceDto.id()).isEqualTo(1L);
        assertThat(balanceDto.cryptoId()).isEqualTo(10L);
        assertThat(balanceDto.externalId()).isEqualTo("bitcoin");
        assertThat(balanceDto.symbol()).isEqualTo("BTC");
        assertThat(balanceDto.cryptoName()).isEqualTo("Bitcoin");
        assertThat(balanceDto.amount()).isEqualByComparingTo("2.0");
        assertThat(balanceDto.currentPriceUsd()).isEqualByComparingTo("50000.00");
        assertThat(balanceDto.totalValueUsd()).isEqualByComparingTo("100000.00");
    }

    @Test
    void toResponseDtoShouldReturnNullWhenEntityIsNull() {
        WalletResponseDto dto = walletMapper.toResponseDto(null);

        assertThat(dto).isNull();
    }

    @Test
    void toBalanceResponseDtoShouldMapFieldsAndCalculateValuesCorrectly() {
        CryptoCurrency eth = CryptoCurrency.builder()
                .id(20L)
                .externalId("ethereum")
                .symbol("ETH")
                .name("Ethereum")
                .priceUsd(new BigDecimal("3000.00"))
                .build();

        WalletBalance balance = WalletBalance.builder()
                .id(5L)
                .cryptoCurrency(eth)
                .amount(new BigDecimal("1.5"))
                .build();

        WalletBalanceResponseDto dto = walletMapper.toBalanceResponseDto(balance);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.cryptoId()).isEqualTo(20L);
        assertThat(dto.externalId()).isEqualTo("ethereum");
        assertThat(dto.symbol()).isEqualTo("ETH");
        assertThat(dto.cryptoName()).isEqualTo("Ethereum");
        assertThat(dto.amount()).isEqualByComparingTo("1.5");
        assertThat(dto.currentPriceUsd()).isEqualByComparingTo("3000.00");
        assertThat(dto.totalValueUsd()).isEqualByComparingTo("4500.00");
    }

    @Test
    void toBalanceResponseDtoShouldReturnNullWhenEntityIsNull() {
        WalletBalanceResponseDto dto = walletMapper.toBalanceResponseDto(null);

        assertThat(dto).isNull();
    }

    @Test
    void getCurrentPriceSafeShouldReturnZeroWhenEntityOrCryptoOrPriceIsNull() {
        assertThat(walletMapper.getCurrentPriceSafe(null)).isEqualByComparingTo(BigDecimal.ZERO);

        WalletBalance balanceWithoutCrypto = WalletBalance.builder().build();
        assertThat(walletMapper.getCurrentPriceSafe(balanceWithoutCrypto)).isEqualByComparingTo(BigDecimal.ZERO);

        CryptoCurrency cryptoWithoutPrice = CryptoCurrency.builder().priceUsd(null).build();
        WalletBalance balanceWithNullPrice = WalletBalance.builder().cryptoCurrency(cryptoWithoutPrice).build();
        assertThat(walletMapper.getCurrentPriceSafe(balanceWithNullPrice)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateTotalValueShouldReturnZeroWhenAnyRequiredFieldIsNull() {
        assertThat(walletMapper.calculateTotalValue(null)).isEqualByComparingTo(BigDecimal.ZERO);

        WalletBalance balanceWithoutAmount = WalletBalance.builder()
                .cryptoCurrency(CryptoCurrency.builder().priceUsd(new BigDecimal("100")).build())
                .amount(null)
                .build();
        assertThat(walletMapper.calculateTotalValue(balanceWithoutAmount)).isEqualByComparingTo(BigDecimal.ZERO);

        WalletBalance balanceWithoutCrypto = WalletBalance.builder()
                .amount(new BigDecimal("10"))
                .cryptoCurrency(null)
                .build();
        assertThat(walletMapper.calculateTotalValue(balanceWithoutCrypto)).isEqualByComparingTo(BigDecimal.ZERO);

        WalletBalance balanceWithoutPrice = WalletBalance.builder()
                .amount(new BigDecimal("10"))
                .cryptoCurrency(CryptoCurrency.builder().priceUsd(null).build())
                .build();
        assertThat(walletMapper.calculateTotalValue(balanceWithoutPrice)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}