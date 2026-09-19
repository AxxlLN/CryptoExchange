package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.CryptoPriceHistory;
import com.crypto.exchange.web.dto.response.CryptoCurrencyResponseDto;
import com.crypto.exchange.web.dto.response.CryptoPriceHistoryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoMapperTest {

    private final CryptoMapper cryptoMapper = Mappers.getMapper(CryptoMapper.class);

    private CryptoCurrency cryptoCurrency;
    private CryptoPriceHistory priceHistory;
    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now(ZoneOffset.UTC);

        cryptoCurrency = CryptoCurrency.builder()
                .id(1L)
                .symbol("BTC")
                .name("Bitcoin")
                .priceUsd(new BigDecimal("60000.00"))
                .updatedAt(now)
                .build();

        priceHistory = CryptoPriceHistory.builder()
                .priceUsd(new BigDecimal("59500.00"))
                .recordedAt(now)
                .build();
    }

    @Nested
    @DisplayName("Маппинг CryptoCurrency -> CryptoCurrencyResponseDto")
    class CryptoCurrencyToDtoTests {

        @Test
        @DisplayName("Успешно маппит CryptoCurrency в CryptoCurrencyResponseDto")
        void toResponseDtoSuccess() {
            CryptoCurrencyResponseDto dto = cryptoMapper.toResponseDto(cryptoCurrency);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.symbol()).isEqualTo("BTC");
            assertThat(dto.name()).isEqualTo("Bitcoin");
            assertThat(dto.priceUsd()).isEqualByComparingTo("60000.00");
            assertThat(dto.updatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Возвращает null при передаче null сущности CryptoCurrency")
        void toResponseDtoNullEntityReturnsNull() {
            assertThat(cryptoMapper.toResponseDto(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Маппинг CryptoPriceHistory -> CryptoPriceHistoryDto")
    class CryptoPriceHistoryToDtoTests {

        @Test
        @DisplayName("Успешно маппит CryptoPriceHistory в CryptoPriceHistoryDto")
        void toHistoryDtoSuccess() {
            CryptoPriceHistoryDto dto = cryptoMapper.toHistoryDto(priceHistory);

            assertThat(dto).isNotNull();
            assertThat(dto.priceUsd()).isEqualByComparingTo("59500.00");
            assertThat(dto.recordedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Возвращает null при передаче null сущности CryptoPriceHistory")
        void toHistoryDtoNullEntityReturnsNull() {
            assertThat(cryptoMapper.toHistoryDto(null)).isNull();
        }
    }
}