package com.crypto.exchange.core.service;

import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.CryptoMapper;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.web.dto.response.CryptoCurrencyResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CryptoService {

    private final CryptoCurrencyRepository cryptoRepository;
    private final CryptoMapper cryptoMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = "cryptoPrices")
    public List<CryptoCurrencyResponseDto> getAllCryptocurrencies() {
        return cryptoRepository.findAll().stream()
                .map(cryptoMapper::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CryptoCurrencyResponseDto getCryptoById(Long id) {
        return cryptoRepository.findById(id)
                .map(cryptoMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cryptocurrency not found with id: " + id));
    }
}