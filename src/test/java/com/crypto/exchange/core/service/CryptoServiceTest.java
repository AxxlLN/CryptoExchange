package com.crypto.exchange.core.service;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.CryptoMapper;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.web.dto.response.CryptoCurrencyResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoServiceTest {

    @Mock
    private CryptoCurrencyRepository cryptoRepository;

    @Mock
    private CryptoMapper cryptoMapper;

    @InjectMocks
    private CryptoService cryptoService;

    @Test
    void getAllCryptocurrencies_shouldReturnListOfDtos() {
        CryptoCurrency crypto = CryptoCurrency.builder()
                .id(1L)
                .symbol("BTC")
                .name("Bitcoin")
                .priceUsd(new BigDecimal("65000.0000"))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(cryptoRepository.findAll()).thenReturn(List.of(crypto));

        CryptoCurrencyResponseDto responseDto = new CryptoCurrencyResponseDto(
                1L, "BTC", "Bitcoin", new BigDecimal("65000.0000"), crypto.getUpdatedAt()
        );
        when(cryptoMapper.toResponseDto(crypto)).thenReturn(responseDto);

        List<CryptoCurrencyResponseDto> result = cryptoService.getAllCryptocurrencies();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).symbol()).isEqualTo("BTC");
        assertThat(result.get(0).priceUsd()).isEqualByComparingTo(new BigDecimal("65000.0000"));

        verify(cryptoRepository, times(1)).findAll();
        verify(cryptoMapper, times(1)).toResponseDto(crypto);
    }

    @Test
    void getCryptoById_shouldReturnDto_whenCryptoExists() {
        Long cryptoId = 1L;
        CryptoCurrency crypto = CryptoCurrency.builder()
                .id(cryptoId)
                .symbol("ETH")
                .name("Ethereum")
                .priceUsd(new BigDecimal("3000.0000"))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(cryptoRepository.findById(cryptoId)).thenReturn(Optional.of(crypto));

        CryptoCurrencyResponseDto responseDto = new CryptoCurrencyResponseDto(
                cryptoId, "ETH", "Ethereum", new BigDecimal("3000.0000"), crypto.getUpdatedAt()
        );
        when(cryptoMapper.toResponseDto(crypto)).thenReturn(responseDto);

        CryptoCurrencyResponseDto result = cryptoService.getCryptoById(cryptoId);

        assertThat(result).isNotNull();
        assertThat(result.symbol()).isEqualTo("ETH");

        verify(cryptoRepository, times(1)).findById(cryptoId);
        verify(cryptoMapper, times(1)).toResponseDto(crypto);
    }

    @Test
    void getCryptoById_shouldThrowException_whenCryptoNotFound() {
        Long cryptoId = 999L;

        when(cryptoRepository.findById(cryptoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cryptoService.getCryptoById(cryptoId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cryptocurrency not found with id: " + cryptoId);

        verify(cryptoRepository, times(1)).findById(cryptoId);
        verifyNoInteractions(cryptoMapper);
    }
}