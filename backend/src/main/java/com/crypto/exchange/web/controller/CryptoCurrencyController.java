package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.service.CryptoPriceHistoryService;
import com.crypto.exchange.core.service.CryptoService;
import com.crypto.exchange.web.dto.response.CryptoCurrencyResponseDto;
import com.crypto.exchange.web.dto.response.CryptoPriceHistoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cryptos")
@RequiredArgsConstructor
@Tag(name = "Cryptocurrency Market", description = "API для получения курсов криптовалют и истории котировок")
public class CryptoCurrencyController {

    private final CryptoService cryptoService;
    private final CryptoPriceHistoryService priceHistoryService;

    @GetMapping
    @Operation(summary = "Получить полный список всех доступных криптовалют с актуальными ценами")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешное получение списка криптовалют")
    })
    public ResponseEntity<List<CryptoCurrencyResponseDto>> getAllCryptos() {
        return ResponseEntity.ok(cryptoService.getAllCryptocurrencies());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить информацию о конкретной криптовалюте по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Криптовалюта найдена"),
            @ApiResponse(responseCode = "404", description = "Криптовалюта с указанным ID не найдена")
    })
    public ResponseEntity<CryptoCurrencyResponseDto> getCryptoById(
            @Parameter(description = "Идентификатор криптовалюты", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(cryptoService.getCryptoById(id));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Получить историю изменения цены криптовалюты по ID (с пагинацией)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешное получение истории цен"),
            @ApiResponse(responseCode = "404", description = "Криптовалюта с указанным ID не найдена")
    })
    public ResponseEntity<Page<CryptoPriceHistoryDto>> getPriceHistory(
            @Parameter(description = "Идентификатор криптовалюты", example = "1")
            @PathVariable Long id,
            @ParameterObject
            @PageableDefault(size = 20, sort = "recordedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(priceHistoryService.getPriceHistory(id, pageable));
    }
}