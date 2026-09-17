package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.service.CryptoPriceHistoryService;
import com.crypto.exchange.core.service.CryptoService;
import com.crypto.exchange.web.dto.response.CryptoCurrencyResponseDto;
import com.crypto.exchange.web.dto.response.CryptoPriceHistoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cryptos")
@RequiredArgsConstructor
@Tag(name = "Cryptocurrency Market API", description = "Управление криптовалютами, котировками и историей цен")
public class CryptoCurrencyController {

    private final CryptoService cryptoService;
    private final CryptoPriceHistoryService priceHistoryService;

    @GetMapping
    @Operation(summary = "Получить список всех доступных криптовалют с актуальными ценами")
    public ResponseEntity<List<CryptoCurrencyResponseDto>> getAllCryptos() {
        return ResponseEntity.ok(cryptoService.getAllCryptocurrencies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить информацию о конкретной криптовалюте по ID")
    public ResponseEntity<CryptoCurrencyResponseDto> getCryptoById(
            @Parameter(description = "ID криптовалюты", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(cryptoService.getCryptoById(id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Получить историю изменения цены криптовалюты по ID")
    public ResponseEntity<Page<CryptoPriceHistoryDto>> getPriceHistory(
            @Parameter(description = "ID криптовалюты", example = "1")
            @PathVariable Long id,
            @ParameterObject
            @PageableDefault(size = 20, sort = "recordedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<CryptoPriceHistoryDto> history = priceHistoryService.getPriceHistory(id, pageable);
        return ResponseEntity.ok(history);
    }
}