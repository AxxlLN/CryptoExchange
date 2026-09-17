package com.crypto.exchange.core.service;

import com.crypto.exchange.web.dto.request.BalanceOperationRequestDto;
import com.crypto.exchange.web.dto.request.CreateWalletRequestDto;
import com.crypto.exchange.web.dto.request.TransferRequestDto;
import com.crypto.exchange.web.dto.response.AggregatedBalanceDto;
import com.crypto.exchange.web.dto.response.TransferResponseDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;

import java.util.List;

public interface WalletService {
    WalletResponseDto createWallet(Long userId, CreateWalletRequestDto request);
    List<WalletResponseDto> getUserWallets(Long userId);
    WalletResponseDto getWalletById(Long userId, Long walletId);
    WalletResponseDto getWalletByAddress(String address);
    WalletResponseDto deposit(Long userId, Long walletId, BalanceOperationRequestDto request);
    WalletResponseDto withdraw(Long userId, Long walletId, BalanceOperationRequestDto request);
    List<AggregatedBalanceDto> getAggregatedBalances(Long userId);
}