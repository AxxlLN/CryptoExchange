package com.crypto.exchange.core.notification;

import com.crypto.exchange.core.entity.Transaction.TransactionType;
import com.crypto.exchange.core.event.TransactionCompletedEvent;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationListenerTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailNotificationListener emailNotificationListener;

    @Test
    void handleTransactionCompleted_shouldSendEmailSuccessfully() {
        TransactionResponseDto txDto = new TransactionResponseDto(
                1L,
                10L,
                20L,
                "BTC",
                "USDT",
                new BigDecimal("1.5"),
                new BigDecimal("65000.00"),
                TransactionType.EXCHANGE,
                TransactionResponseDto.TransactionDirection.OUTGOING,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        TransactionCompletedEvent event = new TransactionCompletedEvent("test@example.com", txDto);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/receipt"), any(Context.class))).thenReturn("<html>Receipt</html>");

        emailNotificationListener.handleTransactionCompleted(event);

        verify(mailSender, times(1)).createMimeMessage();
        verify(templateEngine, times(1)).process(eq("email/receipt"), any(Context.class));
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    void handleTransactionCompleted_shouldSkipWhenEmailIsNullOrBlank() {
        TransactionResponseDto txDto = new TransactionResponseDto(
                1L,
                10L,
                20L,
                "BTC",
                "USDT",
                new BigDecimal("1.5"),
                new BigDecimal("65000.00"),
                TransactionType.EXCHANGE,
                TransactionResponseDto.TransactionDirection.OUTGOING,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        TransactionCompletedEvent eventWithNullEmail = new TransactionCompletedEvent(null, txDto);
        TransactionCompletedEvent eventWithBlankEmail = new TransactionCompletedEvent("   ", txDto);

        emailNotificationListener.handleTransactionCompleted(eventWithNullEmail);
        emailNotificationListener.handleTransactionCompleted(eventWithBlankEmail);

        verifyNoInteractions(mailSender);
        verifyNoInteractions(templateEngine);
    }

    @Test
    void handleTransactionCompleted_shouldCatchExceptionAndNotThrow() {
        TransactionResponseDto txDto = new TransactionResponseDto(
                1L,
                10L,
                20L,
                "BTC",
                "USDT",
                new BigDecimal("1.5"),
                new BigDecimal("65000.00"),
                TransactionType.EXCHANGE,
                TransactionResponseDto.TransactionDirection.OUTGOING,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        TransactionCompletedEvent event = new TransactionCompletedEvent("test@example.com", txDto);

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq("email/receipt"), any(Context.class))).thenReturn("<html>Receipt</html>");

        doThrow(new RuntimeException("SMTP server error")).when(mailSender).send(any(MimeMessage.class));

        emailNotificationListener.handleTransactionCompleted(event);

        verify(mailSender, times(1)).send(mimeMessage);
    }
}