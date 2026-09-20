package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.mapper.TransactionMapper;
import com.crypto.exchange.core.repository.TransactionRepository;
import com.crypto.exchange.core.service.PdfGeneratorService;
import com.crypto.exchange.core.service.TransactionService;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final PdfGeneratorService pdfGeneratorService;

    @Override
    public Page<TransactionResponseDto> getUserTransactions(Long userId, Pageable pageable) {
        return transactionRepository.findAllByUserIdInvolved(userId, pageable)
                .map(transaction -> transactionMapper.toResponseDto(transaction, userId));
    }

    @Override
    public byte[] generatePdfReport(Long userId, String username) {
        List<TransactionResponseDto> transactions = transactionRepository.findAllByUserIdInvolvedList(userId)
                .stream()
                .map(transaction -> transactionMapper.toResponseDto(transaction, userId))
                .toList();

        return pdfGeneratorService.generateTransactionsReport(username, transactions);
    }
}