package com.crypto.exchange.core.repository;

import com.crypto.exchange.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностью пользователей (User).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Поиск пользователя по имени (для процесса аутентификации).
     */
    Optional<User> findByUsername(String username);

    /**
     * Поиск пользователя по email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Удобный метод для авторизации: поиск пользователя по логину ИЛИ email.
     */
    @Query("SELECT u FROM User u WHERE u.username = :login OR u.email = :login")
    Optional<User> findByUsernameOrEmail(@Param("login") String login);

    /**
     * Проверка существования пользователя с таким имя или email.
     */
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /**
     * Агрегирующий расчёт общего баланса в USD.
     * Возвращает BigDecimal для исключения потери точности.
     */
    @Query(value = """
            SELECT COALESCE(SUM(wb.amount * cc.price_usd), 0.0)
            FROM users u
            JOIN wallets w ON w.user_id = u.id
            JOIN wallet_balances wb ON wb.wallet_id = w.id
            JOIN crypto_currencies cc ON cc.id = wb.crypto_id
            WHERE u.id = :userId
            """, nativeQuery = true)
    BigDecimal calculateTotalBalanceInUsdByUserId(@Param("userId") Long userId);
}