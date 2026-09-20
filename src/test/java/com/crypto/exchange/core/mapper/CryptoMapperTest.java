package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.CryptoPriceHistory;
import com.crypto.exchange.web.dto.response.CryptoCurrencyResponseDto;
import com.crypto.exchange.web.dto.response.CryptoPriceHistoryDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoMapperTest {

    private final CryptoMapper cryptoMapper = Mappers.getMapper(CryptoMapper.class);

    @Test
    void toResponseDtoShouldMapAllFieldsCorrectly() {
        OffsetDateTime now = OffsetDateTime.now();
        CryptoCurrency entity = CryptoCurrency.builder()
                .id(1L)
                .externalId("bitcoin")
                .symbol("BTC")
                .name("Bitcoin")
                .priceUsd(new BigDecimal("50000.0000"))
                .updatedAt(now)
                .build();

        CryptoCurrencyResponseDto dto = cryptoMapper.toResponseDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.symbol()).isEqualTo("BTC");
        assertThat(dto.name()).isEqualTo("Bitcoin");
        assertThat(dto.priceUsd()).isEqualTo(new BigDecimal("50000.0000"));
        assertThat(dto.updatedAt()).isEqualTo(now);
    }

    @Test
    void toResponseDtoShouldReturnNullWhenEntityIsNull() {
        CryptoCurrencyResponseDto dto = cryptoMapper.toResponseDto(null);

        assertThat(dto).isNull();
    }

    @Test
    void toHistoryDtoShouldMapAllFieldsCorrectly() {
        OffsetDateTime now = OffsetDateTime.now();
        CryptoPriceHistory entity = CryptoPriceHistory.builder()
                .id(10L)
                .priceUsd(new BigDecimal("65000.5000"))
                .recordedAt(now)
                .build();

        CryptoPriceHistoryDto dto = cryptoMapper.toHistoryDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.priceUsd()).isEqualTo(new BigDecimal("65000.5000"));
        assertThat(dto.recordedAt()).isEqualTo(now);
    }

    @Test
    void toHistoryDtoShouldReturnNullWhenEntityIsNull() {
        CryptoPriceHistoryDto dto = cryptoMapper.toHistoryDto(null);

        assertThat(dto).isNull();
    }
}