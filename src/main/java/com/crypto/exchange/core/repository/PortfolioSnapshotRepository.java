package com.crypto.exchange.core.repository;

import com.crypto.exchange.core.entity.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findAllByUserIdAndRecordedAtAfterOrderByRecordedAtAsc(Long userId, LocalDateTime after);

    Optional<PortfolioSnapshot> findTopByUserIdOrderByRecordedAtDesc(Long userId);
}