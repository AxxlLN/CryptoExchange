package com.crypto.exchange.core.repository;

import com.crypto.exchange.core.entity.CryptoPriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CryptoPriceHistoryRepository extends JpaRepository<CryptoPriceHistory, Long> {

    /**
     * Поиск истории цен по ID криптовалюты с поддержкой пагинации.
     */
    Page<CryptoPriceHistory> findAllByCryptoCurrencyId(Long cryptoId, Pageable pageable);

    /**
     * Поиск истории цен по тикеру (например, "BTC").
     */
    List<CryptoPriceHistory> findByCryptoCurrencySymbolOrderByRecordedAtDesc(String symbol);
}