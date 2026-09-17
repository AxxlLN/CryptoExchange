package com.crypto.exchange.core.notification;

import com.crypto.exchange.core.event.TransactionCompletedEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationListener {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    @EventListener
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        if (event.recipientEmail() == null || event.recipientEmail().isBlank()) {
            log.warn("Пропуск отправки письма: email не указан для транзакции #{}", event.transaction().id());
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            Context context = new Context();
            context.setVariable("tx", event.transaction());

            String htmlContent = templateEngine.process("email/receipt", context);

            helper.setTo(event.recipientEmail());
            helper.setSubject("Чек по транзакции #" + event.transaction().id());
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Квитанция отправлена на {} по транзакции #{}", event.recipientEmail(), event.transaction().id());
        } catch (Exception e) {
            log.error("Ошибка при отправке письма для транзакции #{}", event.transaction().id(), e);
        }
    }
}