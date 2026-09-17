package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.mapper.TransactionMapper;
import com.crypto.exchange.core.repository.TransactionRepository;
import com.crypto.exchange.core.service.TransactionService;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDto> getUserTransactions(Long userId, Pageable pageable) {
        return transactionRepository.findAllByUserIdInvolved(userId, pageable)
                .map(transaction -> transactionMapper.toResponseDto(transaction, userId));
    }
}