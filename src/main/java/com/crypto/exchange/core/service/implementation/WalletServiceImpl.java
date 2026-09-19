package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.core.entity.WalletBalance;
import com.crypto.exchange.core.exception.InsufficientFundsException;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.WalletMapper;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.repository.WalletBalanceRepository;
import com.crypto.exchange.core.repository.WalletRepository;
import com.crypto.exchange.core.service.WalletService;
import com.crypto.exchange.web.dto.request.BalanceOperationRequestDto;
import com.crypto.exchange.web.dto.request.CreateWalletRequestDto;
import com.crypto.exchange.web.dto.response.AggregatedBalanceDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final CryptoCurrencyRepository cryptoCurrencyRepository;
    private final WalletMapper walletMapper;
    private final WalletBalanceRepository walletBalanceRepository;

    @Override
    @Transactional
    public WalletResponseDto createWallet(Long userId, CreateWalletRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        List<Wallet> existingWallets = walletRepository.findAllByUserId(userId);

        boolean shouldBeDefault = existingWallets.isEmpty() || Boolean.TRUE.equals(request.isDefault());

        if (shouldBeDefault && !existingWallets.isEmpty()) {
            existingWallets.forEach(wallet -> {
                if (Boolean.TRUE.equals(wallet.getIsDefault())) {
                    wallet.setIsDefault(false);
                }
            });
            walletRepository.saveAll(existingWallets);
        }

        Wallet wallet = Wallet.builder()
                .name(request.name())
                .isDefault(shouldBeDefault)
                .user(user)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        List<CryptoCurrency> currencies = cryptoCurrencyRepository.findAll();
        currencies.forEach(crypto -> {
            WalletBalance balance = WalletBalance.builder()
                    .wallet(savedWallet)
                    .cryptoCurrency(crypto)
                    .amount(BigDecimal.ZERO)
                    .build();
            savedWallet.getBalances().add(balance);
        });

        Wallet finalWallet = walletRepository.save(savedWallet);

        return walletMapper.toResponseDto(finalWallet);
    }
    @Override
    @Transactional(readOnly = true)
    public List<WalletResponseDto> getUserWallets(Long userId) {
        return walletRepository.findAllByUserId(userId)
                .stream()
                .map(walletMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponseDto getWalletById(Long userId, Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));

        if (!wallet.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Wallet not found with id: " + walletId);
        }

        return walletMapper.toResponseDto(wallet);
    }

    @Override
    @Transactional
    public WalletResponseDto deposit(Long userId, Long walletId, BalanceOperationRequestDto request) {
        Wallet wallet = getWalletAndVerifyOwner(userId, walletId);

        int updatedRows = walletBalanceRepository.addBalanceByExternalIdNative(
                wallet.getId(),
                request.externalId(),
                request.amount()
        );

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Balance entry not found for externalId: " + request.externalId());
        }

        return walletMapper.toResponseDto(walletRepository.findById(walletId).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponseDto getWalletByAddress(String address) {
        Wallet wallet = walletRepository.findByAddress(address)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with address: " + address));
        return walletMapper.toResponseDto(wallet);
    }

    @Override
    @Transactional
    public WalletResponseDto withdraw(Long userId, Long walletId, BalanceOperationRequestDto request) {
        Wallet wallet = getWalletAndVerifyOwner(userId, walletId);

        int updatedRows = walletBalanceRepository.deductBalanceByExternalIdNative(
                wallet.getId(),
                request.externalId(),
                request.amount()
        );

        if (updatedRows == 0) {
            throw new InsufficientFundsException("Insufficient funds or balance entry not found for externalId: " + request.externalId());
        }

        return walletMapper.toResponseDto(walletRepository.findById(walletId).orElseThrow());
    }

    private Wallet getWalletAndVerifyOwner(Long userId, Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletId));
        if (!wallet.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Wallet not found: " + walletId);
        }
        return wallet;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AggregatedBalanceDto> getAggregatedBalances(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return walletBalanceRepository.getAggregatedBalancesByUserId(userId);
    }
}
