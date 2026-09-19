package com.crypto.exchange.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Сущность финансовой транзакции.
 * Хранит историю пополнений, выводов и обменов монет пользователей.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    /**
     * Уникальный идентификатор транзакции (Primary Key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Связь N:1 с пользователем, совершившим операцию.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Кошелек списания средств.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    /**
     * Кошелек зачисления средств.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    /**
     * Связь N:1 с исходной криптовалютой (что пользователь продает/отдает).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_crypto_id", nullable = false)
    private CryptoCurrency fromCrypto;

    /**
     * Связь N:1 с целевой криптовалютой (что пользователь покупает/получает).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_crypto_id", nullable = false)
    private CryptoCurrency toCrypto;

    /**
     * Сумма списания в исходной валюте.
     */
    @Column(name = "from_amount", nullable = false, precision = 19, scale = 8)
    private BigDecimal fromAmount;

    /**
     * Сумма зачисления в целевой валюте.
     */
    @Column(name = "to_amount", nullable = false, precision = 19, scale = 8)
    private BigDecimal toAmount;

    /**
     * Тип проведенной операции (например: DEPOSIT, WITHDRAWAL, EXCHANGE).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /**
     * Дата и время проведения транзакции.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Вложенный Enum для типов транзакций.
     */
    public enum TransactionType {
        DEPOSIT,    /** Пополнение счета */
        WITHDRAWAL, /** Вывод средств */
        EXCHANGE,   /** Обмен одной валюты на другую */
        TRANSFER    /** Перевод на другой кошелек определенной валюты */
    }
}