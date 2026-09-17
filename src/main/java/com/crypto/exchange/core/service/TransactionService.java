package com.crypto.exchange.core.service;

import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    Page<TransactionResponseDto> getUserTransactions(Long userId, Pageable pageable);
}