package com.crypto.exchange.core.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalletTest {

    @Test
    @DisplayName("generateAddress должен генерировать валидный hex-адрес с префиксом 0x, если address равен null")
    void generateAddress_ShouldGenerateAddress_WhenAddressIsNull() {
        Wallet wallet = new Wallet();

        wallet.generateAddress();

        assertThat(wallet.getAddress()).isNotNull();
        assertThat(wallet.getAddress()).startsWith("0x");
        assertThat(wallet.getAddress()).hasSize(34);
        assertThat(wallet.getAddress()).matches("^0x[a-f0-9]{32}$");
    }

    @Test
    @DisplayName("generateAddress не должен перезаписывать существующий адрес, если он уже установлен")
    void generateAddress_ShouldNotOverwrite_WhenAddressIsAlreadySet() {
        String existingAddress = "0xcustomaddress123456789";
        Wallet wallet = Wallet.builder()
                .address(existingAddress)
                .build();

        wallet.generateAddress();

        assertThat(wallet.getAddress()).isEqualTo(existingAddress);
    }
}