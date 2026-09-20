package com.crypto.exchange.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Сущность финансовой транзакции.
 * Хранит историю пополнений, выводов, обменов монет и P2P-переводов.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Пользователь, инициализировавший транзакцию (отправитель при переводе).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Кошелек списания средств (null для внешнего DEPOSIT).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    /**
     * Кошелек зачисления средств (null для внешнего WITHDRAWAL).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    /**
     * Исходная криптовалюта (или единственная валюта при TRANSFER / DEPOSIT / WITHDRAWAL).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_crypto_id")
    private CryptoCurrency fromCrypto;

    /**
     * Целевая криптовалюта (актуально только для EXCHANGE).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_crypto_id")
    private CryptoCurrency toCrypto;

    /**
     * Сумма списания в исходной валюте.
     */
    @Column(name = "from_amount", nullable = false, precision = 19, scale = 8)
    private BigDecimal fromAmount;

    /**
     * Сумма зачисления в целевой валюте (для TRANSFER обычно равна fromAmount).
     */
    @Column(name = "to_amount", nullable = false, precision = 19, scale = 8)
    private BigDecimal toAmount;

    /**
     * Тип проведенной операции.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /**
     * Статус выполнения транзакции.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /**
     * Дата и время проведения транзакции.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (this.status == null) {
            this.status = TransactionStatus.PENDING;
        }
    }

    public enum TransactionType {
        DEPOSIT,    /** Пополнение счета */
        WITHDRAWAL, /** Вывод средств */
        EXCHANGE,   /** Обмен одной валюты на другую */
        TRANSFER    /** Перевод на другой кошелек */
    }

    public enum TransactionStatus {
        PENDING,   /** В обработке */
        SUCCESS,   /** Успешно завершена */
        FAILED,    /** Ошибка выполнения */
        CANCELLED  /** Отменена */
    }
}