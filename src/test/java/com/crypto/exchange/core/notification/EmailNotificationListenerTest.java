package com.crypto.exchange.core.notification;

import com.crypto.exchange.core.event.TransactionCompletedEvent;
import com.crypto.exchange.core.entity.Transaction.TransactionStatus;
import com.crypto.exchange.core.entity.Transaction.TransactionType;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.dto.response.TransactionResponseDto.TransactionDirection;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationListenerTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailNotificationListener emailNotificationListener;

    private TransactionResponseDto createSampleDto(Long id) {
        return new TransactionResponseDto(
                id,
                "0xFROM",
                "0xTO",
                "BTC",
                "USDT",
                new BigDecimal("1.0"),
                new BigDecimal("50000.0"),
                TransactionType.EXCHANGE,
                TransactionStatus.SUCCESS,
                TransactionDirection.OUTGOING,
                OffsetDateTime.now()
        );
    }

    @Test
    void handleTransactionCompletedShouldSendEmailSuccessfully() {
        TransactionResponseDto transactionDto = createSampleDto(100L);
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                "user@example.com",
                transactionDto
        );

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/receipt"), any(IContext.class)))
                .thenReturn("<html>Receipt</html>");

        emailNotificationListener.handleTransactionCompleted(event);

        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(eq("email/receipt"), any(IContext.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void handleTransactionCompletedShouldSkipWhenEmailIsInvalid(String invalidEmail) {
        TransactionResponseDto transactionDto = createSampleDto(100L);
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                invalidEmail,
                transactionDto
        );

        emailNotificationListener.handleTransactionCompleted(event);

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
        verify(templateEngine, never()).process(any(String.class), any(IContext.class));
    }

    @Test
    void handleTransactionCompletedShouldCatchExceptionWhenMailSendingFails() {
        TransactionResponseDto transactionDto = createSampleDto(100L);
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                "user@example.com",
                transactionDto
        );

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/receipt"), any(IContext.class)))
                .thenReturn("<html>Receipt</html>");

        doThrow(new RuntimeException("Mail server down"))
                .when(mailSender).send(mimeMessage);

        emailNotificationListener.handleTransactionCompleted(event);

        verify(mailSender).send(mimeMessage);
    }
}