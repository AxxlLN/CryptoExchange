package com.crypto.exchange.web.controller;

import com.crypto.exchange.core.service.TransactionService;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "API для работы с историей транзакций")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "Получить историю транзакций текущего пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешное получение списка транзакций"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<Page<TransactionResponseDto>> getUserTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(transactionService.getUserTransactions(principal.getId(), pageable));
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Скачать выписку по транзакциям в формате PDF")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "PDF-файл выписки успешно сгенерирован",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))
            ),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<byte[]> exportTransactionsToPdf(@AuthenticationPrincipal UserPrincipal principal) {
        byte[] pdfBytes = transactionService.generatePdfReport(principal.getId(), principal.getUsername());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }
}