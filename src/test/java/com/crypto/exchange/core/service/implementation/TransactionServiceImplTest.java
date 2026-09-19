package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.Transaction;
import com.crypto.exchange.core.mapper.TransactionMapper;
import com.crypto.exchange.core.repository.TransactionRepository;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Long userId;
    private Pageable pageable;
    private Transaction transaction1;
    private Transaction transaction2;
    private TransactionResponseDto responseDto1;
    private TransactionResponseDto responseDto2;

    @BeforeEach
    void setUp() {
        userId = 1L;
        pageable = PageRequest.of(0, 10);

        transaction1 = Transaction.builder().id(100L).build();
        transaction2 = Transaction.builder().id(200L).build();

        responseDto1 = new TransactionResponseDto(
                100L, 10L, 20L, "BTC", "BTC",
                new BigDecimal("1.0"), new BigDecimal("1.0"),
                Transaction.TransactionType.TRANSFER,
                TransactionResponseDto.TransactionDirection.OUTGOING,
                null
        );

        responseDto2 = new TransactionResponseDto(
                200L, 30L, 10L, "ETH", "ETH",
                new BigDecimal("5.0"), new BigDecimal("5.0"),
                Transaction.TransactionType.TRANSFER,
                TransactionResponseDto.TransactionDirection.INCOMING,
                null
        );
    }

    @Test
    @DisplayName("Возвращает пагинированный список транзакций пользователя с маппингом в DTO")
    void getUserTransactionsReturnsPageOfTransactionResponseDto() {
        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction1, transaction2), pageable, 2);

        when(transactionRepository.findAllByUserIdInvolved(userId, pageable)).thenReturn(transactionPage);
        when(transactionMapper.toResponseDto(transaction1, userId)).thenReturn(responseDto1);
        when(transactionMapper.toResponseDto(transaction2, userId)).thenReturn(responseDto2);

        Page<TransactionResponseDto> result = transactionService.getUserTransactions(userId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).containsExactly(responseDto1, responseDto2);

        verify(transactionRepository).findAllByUserIdInvolved(userId, pageable);
        verify(transactionMapper).toResponseDto(transaction1, userId);
        verify(transactionMapper).toResponseDto(transaction2, userId);
    }

    @Test
    @DisplayName("Возвращает пустую страницу, если у пользователя нет транзакций")
    void getUserTransactionsEmptyPageReturnsEmpty() {
        Page<Transaction> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(transactionRepository.findAllByUserIdInvolved(userId, pageable)).thenReturn(emptyPage);

        Page<TransactionResponseDto> result = transactionService.getUserTransactions(userId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isZero();

        verify(transactionRepository).findAllByUserIdInvolved(userId, pageable);
    }
}