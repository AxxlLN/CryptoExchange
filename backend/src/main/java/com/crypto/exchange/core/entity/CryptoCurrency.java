package com.crypto.exchange.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

/**
 * Сущность криптовалюты (справочник доступных монет).
 * Участвует в связи N:M с кошельками через промежуточную сущность WalletBalance.
 */
@Entity
@Table(name = "crypto_currencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CryptoCurrency {

    /**
     * Уникальный идентификатор криптовалюты (Primary Key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Внешний идентификатор монеты из стороннего API
     */
    @Column(name = "external_id", unique = true, length = 50)
    private String externalId;

    /**
     * Символьный тикер монеты (например: BTC, ETH, USDT).
     */
    @Column(nullable = false, unique = true, length = 10)
    private String symbol;

    /**
     * Полное название криптовалюты (например: Bitcoin, Ethereum).
     */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * Текущий курс криптовалюты к USD с точностью до 4 знаков после запятой.
     */
    @Column(name = "price_usd", nullable = false, precision = 19, scale = 4)
    private BigDecimal priceUsd;

    /**
     * Дата и время последнего обновления курса.
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Связь 1:N с промежуточной таблицей балансов (часть связи N:M с Wallet).
     */
    @Builder.Default
    @OneToMany(mappedBy = "cryptoCurrency", fetch = FetchType.LAZY)
    private Set<WalletBalance> balances = new HashSet<>();

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}