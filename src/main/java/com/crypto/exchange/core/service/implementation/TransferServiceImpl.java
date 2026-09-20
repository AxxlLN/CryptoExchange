package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.Transaction;
import com.crypto.exchange.core.entity.Transaction.TransactionStatus;
import com.crypto.exchange.core.entity.Transaction.TransactionType;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.core.entity.WalletBalance;
import com.crypto.exchange.core.event.TransactionCompletedEvent;
import com.crypto.exchange.core.exception.InsufficientFundsException;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.mapper.TransactionMapper;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.TransactionRepository;
import com.crypto.exchange.core.repository.WalletBalanceRepository;
import com.crypto.exchange.core.repository.WalletRepository;
import com.crypto.exchange.core.service.TransferService;
import com.crypto.exchange.web.dto.request.TransferRequestDto;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.dto.response.TransferResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final CryptoCurrencyRepository cryptoCurrencyRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public TransferResponseDto transfer(Long userId, TransferRequestDto request) {
        Wallet senderWallet = walletRepository.findByAddress(request.fromAddress())
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found with address: " + request.fromAddress()));

        if (!senderWallet.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Sender wallet not found with address: " + request.fromAddress());
        }

        Wallet recipientWallet = walletRepository.findByAddress(request.recipientAddress())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recipient wallet not found with address: " + request.recipientAddress()));

        if (senderWallet.getId().equals(recipientWallet.getId())) {
            throw new IllegalArgumentException("Cannot transfer funds to the same wallet");
        }

        CryptoCurrency crypto = cryptoCurrencyRepository.findByExternalId(request.cryptoExternalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Crypto currency not found with externalId: " + request.cryptoExternalId()));

        int deducted = walletBalanceRepository.deductBalanceNative(
                senderWallet.getId(),
                crypto.getId(),
                request.amount()
        );

        if (deducted == 0) {
            throw new InsufficientFundsException("Insufficient funds or balance entry not found for: " + crypto.getSymbol());
        }

        int added = walletBalanceRepository.addBalanceNative(
                recipientWallet.getId(),
                crypto.getId(),
                request.amount()
        );

        if (added == 0) {
            WalletBalance newBalance = WalletBalance.builder()
                    .wallet(recipientWallet)
                    .cryptoCurrency(crypto)
                    .amount(request.amount())
                    .build();
            walletBalanceRepository.save(newBalance);
        }

        Transaction transaction = Transaction.builder()
                .user(senderWallet.getUser())
                .fromWallet(senderWallet)
                .toWallet(recipientWallet)
                .fromCrypto(crypto)
                .toCrypto(crypto)
                .fromAmount(request.amount())
                .toAmount(request.amount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        TransactionResponseDto responseDto = transactionMapper.toResponseDto(savedTransaction, userId);
        eventPublisher.publishEvent(new TransactionCompletedEvent(
                senderWallet.getUser().getEmail(),
                responseDto
        ));

        return transactionMapper.toTransferResponseDto(savedTransaction);
    }
}