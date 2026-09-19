package com.crypto.exchange.core.service;

import com.crypto.exchange.core.entity.CryptoPriceHistory;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.CryptoMapper;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.CryptoPriceHistoryRepository;
import com.crypto.exchange.web.dto.response.CryptoPriceHistoryDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoPriceHistoryServiceTest {

    @Mock
    private CryptoPriceHistoryRepository priceHistoryRepository;

    @Mock
    private CryptoCurrencyRepository cryptoCurrencyRepository;

    @Mock
    private CryptoMapper cryptoMapper;

    @InjectMocks
    private CryptoPriceHistoryService cryptoPriceHistoryService;

    @Test
    void getPriceHistory_shouldReturnPageOfDtos_whenCryptoExists() {
        Long cryptoId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        when(cryptoCurrencyRepository.existsById(cryptoId)).thenReturn(true);

        CryptoPriceHistory historyEntity = new CryptoPriceHistory();
        historyEntity.setId(100L);
        historyEntity.setPriceUsd(new BigDecimal("65000.0000"));
        historyEntity.setRecordedAt(OffsetDateTime.now(ZoneOffset.UTC));

        Page<CryptoPriceHistory> historyPage = new PageImpl<>(List.of(historyEntity), pageable, 1);
        when(priceHistoryRepository.findAllByCryptoCurrencyId(eq(cryptoId), eq(pageable))).thenReturn(historyPage);

        CryptoPriceHistoryDto historyDto = new CryptoPriceHistoryDto(
                new BigDecimal("65000.0000"),
                historyEntity.getRecordedAt()
        );
        when(cryptoMapper.toHistoryDto(historyEntity)).thenReturn(historyDto);

        Page<CryptoPriceHistoryDto> result = cryptoPriceHistoryService.getPriceHistory(cryptoId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).priceUsd()).isEqualByComparingTo(new BigDecimal("65000.0000"));

        verify(cryptoCurrencyRepository, times(1)).existsById(cryptoId);
        verify(priceHistoryRepository, times(1)).findAllByCryptoCurrencyId(cryptoId, pageable);
        verify(cryptoMapper, times(1)).toHistoryDto(historyEntity);
    }

    @Test
    void getPriceHistory_shouldThrowException_whenCryptoNotFound() {
        Long cryptoId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        when(cryptoCurrencyRepository.existsById(cryptoId)).thenReturn(false);

        assertThatThrownBy(() -> cryptoPriceHistoryService.getPriceHistory(cryptoId, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cryptocurrency not found with id: " + cryptoId);

        verify(cryptoCurrencyRepository, times(1)).existsById(cryptoId);
        verifyNoInteractions(priceHistoryRepository);
        verifyNoInteractions(cryptoMapper);
    }
}