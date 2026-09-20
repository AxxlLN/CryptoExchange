package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.service.TransferService;
import com.crypto.exchange.web.dto.request.TransferRequestDto;
import com.crypto.exchange.web.dto.response.TransferResponseDto;
import com.crypto.exchange.web.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Управление P2P переводами средств")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("isAuthenticated()")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Выполнить P2P перевод криптовалюты по адресу кошелька")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Перевод успешно выполнен"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации входящих данных (неверная сумма или адрес)"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован"),
            @ApiResponse(responseCode = "404", description = "Кошелек отправителя или получателя не найден"),
            @ApiResponse(responseCode = "422", description = "Недостаточно средств на балансе для совершения перевода")
    })
    public ResponseEntity<TransferResponseDto> transfer(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TransferRequestDto request
    ) {
        TransferResponseDto response = transferService.transfer(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}