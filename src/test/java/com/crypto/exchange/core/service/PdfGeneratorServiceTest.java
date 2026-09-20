package com.crypto.exchange.core.service;

import com.crypto.exchange.core.entity.Transaction.TransactionStatus;
import com.crypto.exchange.core.entity.Transaction.TransactionType;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.dto.response.TransactionResponseDto.TransactionDirection;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PdfGeneratorServiceTest {

    private PdfGeneratorService pdfGeneratorService;

    @BeforeEach
    void setUp() {
        pdfGeneratorService = new PdfGeneratorService();
    }

    @Test
    @DisplayName("Должен успешно генерировать PDF-документ и не возвращать пустой массив байтов")
    void generateTransactionsReportShouldReturnValidPdfByteArray() throws IOException {
        String username = "john_doe";
        List<TransactionResponseDto> transactions = List.of(
                new TransactionResponseDto(
                        101L,
                        "0x123abc456def",
                        "0x789xyz",
                        "BTC",
                        "USDT",
                        new BigDecimal("1.500000"),
                        new BigDecimal("97500.00"),
                        TransactionType.DEPOSIT,
                        TransactionStatus.SUCCESS,
                        TransactionDirection.INCOMING,
                        OffsetDateTime.of(2026, 9, 20, 12, 0, 0, 0, ZoneOffset.UTC)
                )
        );

        byte[] pdfBytes = pdfGeneratorService.generateTransactionsReport(username, transactions);

        assertThat(pdfBytes).isNotNull().isNotEmpty();

        String pdfHeader = new String(pdfBytes, 0, 5);
        assertThat(pdfHeader).isEqualTo("%PDF-");

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
             PdfDocument pdfDoc = new PdfDocument(reader)) {

            assertThat(pdfDoc.getNumberOfPages()).isEqualTo(1);

            String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1));
            assertThat(pageText)
                    .contains("Crypto Exchange - Transaction History")
                    .contains("User: john_doe")
                    .contains("Total Records: 1")
                    .contains("101")
                    .contains("DEPOSIT")
                    .contains("INCOMING")
                    .contains("1.5 BTC")
                    .contains("SUCCESS")
                    .contains("0x789xyz")
                    .contains("2026-09-20 12:00:00");
        }
    }

    @Test
    @DisplayName("Должен корректно формировать отчет для пустого списка транзакций")
    void generateTransactionsReportShouldHandleEmptyTransactionList() throws IOException {
        String username = "alice_smith";
        List<TransactionResponseDto> transactions = Collections.emptyList();

        byte[] pdfBytes = pdfGeneratorService.generateTransactionsReport(username, transactions);

        assertThat(pdfBytes).isNotNull().isNotEmpty();

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
             PdfDocument pdfDoc = new PdfDocument(reader)) {

            String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1));
            assertThat(pageText)
                    .contains("User: alice_smith")
                    .contains("Total Records: 0");
        }
    }

    @Test
    @DisplayName("Должен корректно обрабатывать транзакции с null-полями без выбрасывания NPE")
    void generateTransactionsReportShouldHandleNullFieldsInTransactionDto() {
        String username = "bob_builder";
        TransactionResponseDto nullableTx = new TransactionResponseDto(
                202L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertDoesNotThrow(() -> {
            byte[] pdfBytes = pdfGeneratorService.generateTransactionsReport(username, List.of(nullableTx));
            assertThat(pdfBytes).isNotNull().isNotEmpty();
        });
    }

    @Test
    @DisplayName("Должен брать fromWalletAddress, если toWalletAddress равен null")
    void generateTransactionsReportShouldFallbackToFromWalletAddressWhenToWalletIsNull() throws IOException {
        String username = "charlie_brown";
        TransactionResponseDto tx = new TransactionResponseDto(
                303L,
                "0xFromWalletAddressOnly",
                null,
                "USDT",
                null,
                new BigDecimal("500.00"),
                null,
                TransactionType.WITHDRAWAL,
                TransactionStatus.PENDING,
                TransactionDirection.OUTGOING,
                OffsetDateTime.of(2026, 9, 20, 14, 30, 0, 0, ZoneOffset.UTC)
        );

        byte[] pdfBytes = pdfGeneratorService.generateTransactionsReport(username, List.of(tx));

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
             PdfDocument pdfDoc = new PdfDocument(reader)) {

            String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1));
            assertThat(pageText)
                    .contains("0xFromWalletAddressOnly")
                    .contains("PENDING")
                    .contains("WITHDRAWAL");
        }
    }
}