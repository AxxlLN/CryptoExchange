package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.Transaction;
import com.crypto.exchange.core.mapper.TransactionMapper;
import com.crypto.exchange.core.repository.TransactionRepository;
import com.crypto.exchange.core.service.PdfGeneratorService;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private PdfGeneratorService pdfGeneratorService;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Long userId;
    private String username;
    private Transaction transaction1;
    private Transaction transaction2;
    private TransactionResponseDto dto1;
    private TransactionResponseDto dto2;

    @BeforeEach
    void setUp() {
        userId = 1L;
        username = "john_doe";

        transaction1 = Transaction.builder().id(100L).build();
        transaction2 = Transaction.builder().id(101L).build();

        dto1 = new TransactionResponseDto(
                100L,
                "0x111",
                "0x222",
                "BTC",
                "BTC",
                new BigDecimal("1.0"),
                new BigDecimal("1.0"),
                null,
                null,
                TransactionResponseDto.TransactionDirection.OUTGOING,
                OffsetDateTime.now()
        );

        dto2 = new TransactionResponseDto(
                101L,
                "0x333",
                "0x111",
                "ETH",
                "ETH",
                new BigDecimal("5.0"),
                new BigDecimal("5.0"),
                null,
                null,
                TransactionResponseDto.TransactionDirection.INCOMING,
                OffsetDateTime.now()
        );
    }

    @Test
    void getUserTransactionsShouldReturnPagedTransactionsResponseDto() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction1, transaction2), pageable, 2);

        when(transactionRepository.findAllByUserIdInvolved(userId, pageable)).thenReturn(transactionPage);
        when(transactionMapper.toResponseDto(transaction1, userId)).thenReturn(dto1);
        when(transactionMapper.toResponseDto(transaction2, userId)).thenReturn(dto2);

        Page<TransactionResponseDto> result = transactionService.getUserTransactions(userId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).containsExactly(dto1, dto2);

        verify(transactionRepository).findAllByUserIdInvolved(userId, pageable);
        verify(transactionMapper).toResponseDto(transaction1, userId);
        verify(transactionMapper).toResponseDto(transaction2, userId);
    }

    @Test
    void getUserTransactionsShouldReturnEmptyPageWhenNoTransactionsFound() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(transactionRepository.findAllByUserIdInvolved(userId, pageable)).thenReturn(emptyPage);

        Page<TransactionResponseDto> result = transactionService.getUserTransactions(userId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(transactionRepository).findAllByUserIdInvolved(userId, pageable);
    }

    @Test
    void generatePdfReportShouldReturnByteArrayReportForUserTransactions() {
        List<Transaction> transactions = List.of(transaction1, transaction2);
        byte[] expectedPdfBytes = new byte[]{1, 2, 3, 4, 5};

        when(transactionRepository.findAllByUserIdInvolvedList(userId)).thenReturn(transactions);
        when(transactionMapper.toResponseDto(transaction1, userId)).thenReturn(dto1);
        when(transactionMapper.toResponseDto(transaction2, userId)).thenReturn(dto2);
        when(pdfGeneratorService.generateTransactionsReport(username, List.of(dto1, dto2)))
                .thenReturn(expectedPdfBytes);

        byte[] result = transactionService.generatePdfReport(userId, username);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedPdfBytes);

        verify(transactionRepository).findAllByUserIdInvolvedList(userId);
        verify(pdfGeneratorService).generateTransactionsReport(username, List.of(dto1, dto2));
    }

    @Test
    void generatePdfReportShouldHandleEmptyTransactionList() {
        byte[] expectedPdfBytes = new byte[]{0};

        when(transactionRepository.findAllByUserIdInvolvedList(userId)).thenReturn(Collections.emptyList());
        when(pdfGeneratorService.generateTransactionsReport(eq(username), eq(Collections.emptyList())))
                .thenReturn(expectedPdfBytes);

        byte[] result = transactionService.generatePdfReport(userId, username);

        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedPdfBytes);

        verify(pdfGeneratorService).generateTransactionsReport(username, Collections.emptyList());
    }
}