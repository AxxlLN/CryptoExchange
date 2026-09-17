package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.service.WalletService;
import com.crypto.exchange.web.dto.request.BalanceOperationRequestDto;
import com.crypto.exchange.web.dto.request.CreateWalletRequestDto;
import com.crypto.exchange.web.dto.request.TransferRequestDto;
import com.crypto.exchange.web.dto.response.AggregatedBalanceDto;
import com.crypto.exchange.web.dto.response.TransferResponseDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;
import com.crypto.exchange.web.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Управление мультивалютными кошельками")
@SecurityRequirement(name = "Bearer Authentication")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Создать новый кошелек")
    public ResponseEntity<WalletResponseDto> createWallet(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateWalletRequestDto request
    ) {
        WalletResponseDto response = walletService.createWallet(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Получить список всех кошельков текущего пользователя")
    public ResponseEntity<List<WalletResponseDto>> getMyWallets(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(walletService.getUserWallets(currentUser.getId()));
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Получить кошелек по ID")
    public ResponseEntity<WalletResponseDto> getWalletById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long walletId
    ) {
        return ResponseEntity.ok(walletService.getWalletById(currentUser.getId(), walletId));
    }

    @PostMapping("/{walletId}/deposit")
    @Operation(summary = "Пополнить баланс кошелька")
    public ResponseEntity<WalletResponseDto> deposit(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequestDto request
    ) {
        return ResponseEntity.ok(walletService.deposit(currentUser.getId(), walletId, request));
    }

    @PostMapping("/{walletId}/withdraw")
    @Operation(summary = "Списать средства с кошелька")
    public ResponseEntity<WalletResponseDto> withdraw(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long walletId,
            @Valid @RequestBody BalanceOperationRequestDto request
    ) {
        return ResponseEntity.ok(walletService.withdraw(currentUser.getId(), walletId, request));
    }

    @GetMapping("/by-address/{address}")
    @Operation(summary = "Найти кошелек по его публичному адресу")
    public ResponseEntity<WalletResponseDto> getWalletByAddress(@PathVariable String address) {
        return ResponseEntity.ok(walletService.getWalletByAddress(address));
    }

    @GetMapping("/balances/summary")
    @Operation(summary = "Получить суммарный баланс по всем криптовалютам со всех кошельков")
    public ResponseEntity<List<AggregatedBalanceDto>> getAggregatedBalances(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(walletService.getAggregatedBalances(currentUser.getId()));
    }
}