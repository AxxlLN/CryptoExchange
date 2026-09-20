package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.service.WalletService;
import com.crypto.exchange.web.dto.request.BalanceOperationRequestDto;
import com.crypto.exchange.web.dto.request.CreateWalletRequestDto;
import com.crypto.exchange.web.dto.response.AggregatedBalanceDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Управление мультивалютными кошельками")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("isAuthenticated()")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Создать новый кошелек")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Кошелек успешно создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<WalletResponseDto> createWallet(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateWalletRequestDto request
    ) {
        WalletResponseDto response = walletService.createWallet(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Получить список всех кошельков текущего пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список кошельков успешно получен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<List<WalletResponseDto>> getMyWallets(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(walletService.getUserWallets(currentUser.getId()));
    }

    @GetMapping("/{address}")
    @Operation(summary = "Получить кошелек по адресу")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Кошелек найден"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован"),
            @ApiResponse(responseCode = "404", description = "Кошелек не найден или не принадлежит пользователю")
    })
    public ResponseEntity<WalletResponseDto> getWalletByAddress(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String address
    ) {
        return ResponseEntity.ok(walletService.getWalletByAddressAndUser(currentUser.getId(), address));
    }

    @PostMapping("/{address}/deposit")
    @Operation(summary = "Пополнить баланс кошелька по адресу")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Баланс успешно пополнен"),
            @ApiResponse(responseCode = "400", description = "Некорректная сумма или валюта"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован"),
            @ApiResponse(responseCode = "404", description = "Кошелек не найден или не принадлежит пользователю")
    })
    public ResponseEntity<WalletResponseDto> deposit(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String address,
            @Valid @RequestBody BalanceOperationRequestDto request
    ) {
        return ResponseEntity.ok(walletService.deposit(currentUser.getId(), address, request));
    }

    @PostMapping("/{address}/withdraw")
    @Operation(summary = "Списать средства с кошелька по адресу")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Средства успешно списаны"),
            @ApiResponse(responseCode = "400", description = "Недостаточно средств или некорректная сумма"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован"),
            @ApiResponse(responseCode = "404", description = "Кошелек не найден или не принадлежит пользователю")
    })
    public ResponseEntity<WalletResponseDto> withdraw(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String address,
            @Valid @RequestBody BalanceOperationRequestDto request
    ) {
        return ResponseEntity.ok(walletService.withdraw(currentUser.getId(), address, request));
    }

    @GetMapping("/balances/summary")
    @Operation(summary = "Получить суммарный баланс по всем криптовалютам со всех кошельков")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Сводный баланс успешно получен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<List<AggregatedBalanceDto>> getAggregatedBalances(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(walletService.getAggregatedBalances(currentUser.getId()));
    }
}