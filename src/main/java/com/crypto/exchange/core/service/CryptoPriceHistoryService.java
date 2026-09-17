package com.crypto.exchange.core.service;

import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.CryptoMapper;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.CryptoPriceHistoryRepository;
import com.crypto.exchange.web.dto.response.CryptoPriceHistoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoPriceHistoryService {

    private final CryptoPriceHistoryRepository priceHistoryRepository;
    private final CryptoCurrencyRepository cryptoCurrencyRepository;
    private final CryptoMapper cryptoMapper;

    /**
     * Возвращает пагинированный список истории цен для выбранной криптовалюты.
     *
     * @param cryptoId ID криптовалюты
     * @param pageable параметры пагинации
     * @return страница с элементами истории DTO
     */
    @Transactional(readOnly = true)
    public Page<CryptoPriceHistoryDto> getPriceHistory(Long cryptoId, Pageable pageable) {
        log.debug("Fetching price history for crypto ID: {} with pageable: {}", cryptoId, pageable);

        if (!cryptoCurrencyRepository.existsById(cryptoId)) {
            throw new ResourceNotFoundException("Cryptocurrency not found with id: " + cryptoId);
        }

        return priceHistoryRepository.findAllByCryptoCurrencyId(cryptoId, pageable)
                .map(cryptoMapper::toHistoryDto);
    }
}