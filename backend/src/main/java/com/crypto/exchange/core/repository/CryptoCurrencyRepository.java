package com.crypto.exchange.core.repository;

import com.crypto.exchange.core.entity.CryptoCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий справочника криптовалют (CryptoCurrency).
 */
@Repository
public interface CryptoCurrencyRepository extends JpaRepository<CryptoCurrency, Long> {

    /**
     * Поиск монеты по внешнему ID стороннего API.
     */
    Optional<CryptoCurrency> findByExternalId(String externalId);

    /**
     * Пакетная выборка монет по списку внешних ID.
     */
    List<CryptoCurrency> findAllByExternalIdIn(Collection<String> externalIds);

    /**
     * Поиск монеты по символьному тикеру (например, "BTC", "ETH").
     */
    Optional<CryptoCurrency> findBySymbol(String symbol);

    /**
     * Пакетная выборка монет по списку тикеров.
     */
    List<CryptoCurrency> findAllBySymbolIn(Collection<String> symbols);

    /**
     * Проверка существования тикера.
     */
    boolean existsBySymbol(String symbol);

    /**
     * Нативный SQL-запрос: пакетное/быстрое обновление курса монеты с простановкой текущего времени.
     */
    @Modifying
    @Query(value = """
            UPDATE crypto_currencies 
            SET price_usd = :newPrice, updated_at = NOW() 
            WHERE symbol = :symbol
            """, nativeQuery = true)
    int updatePriceNative(@Param("symbol") String symbol, @Param("newPrice") BigDecimal newPrice);}