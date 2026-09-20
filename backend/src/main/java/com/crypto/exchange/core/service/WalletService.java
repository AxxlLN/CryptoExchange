package com.crypto.exchange.core.service;

import com.crypto.exchange.web.dto.request.BalanceOperationRequestDto;
import com.crypto.exchange.web.dto.request.CreateWalletRequestDto;
import com.crypto.exchange.web.dto.response.AggregatedBalanceDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;

import java.util.List;

public interface WalletService {
    WalletResponseDto createWallet(Long userId, CreateWalletRequestDto request);
    List<WalletResponseDto> getUserWallets(Long userId);
    WalletResponseDto getWalletByAddressAndUser(Long userId, String address);
    WalletResponseDto deposit(Long userId, String address, BalanceOperationRequestDto request);
    WalletResponseDto withdraw(Long userId, String address, BalanceOperationRequestDto request);
    List<AggregatedBalanceDto> getAggregatedBalances(Long userId);
}