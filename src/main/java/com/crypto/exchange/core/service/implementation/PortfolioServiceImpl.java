package com.crypto.exchange.core.service.implementation;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.PortfolioSnapshot;
import com.crypto.exchange.core.entity.User;
import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.core.entity.WalletBalance;
import com.crypto.exchange.core.exception.ResourceNotFoundException;
import com.crypto.exchange.core.repository.PortfolioSnapshotRepository;
import com.crypto.exchange.core.repository.UserRepository;
import com.crypto.exchange.core.repository.WalletRepository;
import com.crypto.exchange.core.service.PortfolioService;
import com.crypto.exchange.web.dto.response.DetailedPortfolioDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;

    @Override
    @Transactional(readOnly = true)
    public DetailedPortfolioDto getDetailedPortfolio(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<Wallet> wallets = walletRepository.findAllByUserId(userId);

        var assetTotals = new HashMap<CryptoCurrency, BigDecimal>();
        var assetPrices = new HashMap<CryptoCurrency, BigDecimal>();

        for (Wallet wallet : wallets) {
            for (WalletBalance wb : wallet.getBalances()) {
                CryptoCurrency crypto = wb.getCryptoCurrency();
                assetTotals.put(crypto, assetTotals.getOrDefault(crypto, BigDecimal.ZERO).add(wb.getAmount()));
                assetPrices.put(crypto, crypto.getPriceUsd() != null ? crypto.getPriceUsd() : BigDecimal.ZERO);
            }
        }

        BigDecimal totalPortfolioUsd = BigDecimal.ZERO;
        for (var entry : assetTotals.entrySet()) {
            totalPortfolioUsd = totalPortfolioUsd.add(entry.getValue().multiply(assetPrices.get(entry.getKey())));
        }

        BigDecimal finalTotalPortfolioUsd = totalPortfolioUsd;
        List<DetailedPortfolioDto.CryptoAssetBalanceDto> assetDtos = assetTotals.entrySet().stream().map(entry -> {
            CryptoCurrency crypto = entry.getKey();
            BigDecimal amount = entry.getValue();
            BigDecimal price = assetPrices.get(crypto);
            BigDecimal valueUsd = amount.multiply(price);

            BigDecimal percentage = BigDecimal.ZERO;
            if (finalTotalPortfolioUsd.compareTo(BigDecimal.ZERO) > 0) {
                percentage = valueUsd.multiply(BigDecimal.valueOf(100))
                        .divide(finalTotalPortfolioUsd, 2, RoundingMode.HALF_UP);
            }

            return new DetailedPortfolioDto.CryptoAssetBalanceDto(
                    crypto.getId(),
                    crypto.getSymbol(),
                    crypto.getName(),
                    amount,
                    price,
                    valueUsd,
                    percentage
            );
        }).toList();

        LocalDateTime now = LocalDateTime.now();
        PortfolioSnapshot snapshot24h = portfolioSnapshotRepository
                .findAllByUserIdAndRecordedAtAfterOrderByRecordedAtAsc(userId, now.minusHours(24))
                .stream().findFirst().orElse(null);

        PortfolioSnapshot snapshot7d = portfolioSnapshotRepository
                .findAllByUserIdAndRecordedAtAfterOrderByRecordedAtAsc(userId, now.minusDays(7))
                .stream().findFirst().orElse(null);

        BigDecimal pnl24hUsd = BigDecimal.ZERO;
        BigDecimal pnl24hPercentage = BigDecimal.ZERO;
        if (snapshot24h != null) {
            pnl24hUsd = totalPortfolioUsd.subtract(snapshot24h.getTotalBalanceUsd());
            if (snapshot24h.getTotalBalanceUsd().compareTo(BigDecimal.ZERO) > 0) {
                pnl24hPercentage = pnl24hUsd.multiply(BigDecimal.valueOf(100))
                        .divide(snapshot24h.getTotalBalanceUsd(), 2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal pnl7dUsd = BigDecimal.ZERO;
        BigDecimal pnl7dPercentage = BigDecimal.ZERO;
        if (snapshot7d != null) {
            pnl7dUsd = totalPortfolioUsd.subtract(snapshot7d.getTotalBalanceUsd());
            if (snapshot7d.getTotalBalanceUsd().compareTo(BigDecimal.ZERO) > 0) {
                pnl7dPercentage = pnl7dUsd.multiply(BigDecimal.valueOf(100))
                        .divide(snapshot7d.getTotalBalanceUsd(), 2, RoundingMode.HALF_UP);
            }
        }

        DetailedPortfolioDto.PortfolioAnalyticsDto analyticsDto = new DetailedPortfolioDto.PortfolioAnalyticsDto(
                pnl24hUsd, pnl24hPercentage, pnl7dUsd, pnl7dPercentage
        );

        return new DetailedPortfolioDto(totalPortfolioUsd, assetDtos, analyticsDto);
    }

    @Override
    @Transactional
    public void takeSnapshot(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        List<Wallet> wallets = walletRepository.findAllByUserId(userId);
        BigDecimal totalUsd = BigDecimal.ZERO;

        for (Wallet wallet : wallets) {
            for (WalletBalance wb : wallet.getBalances()) {
                BigDecimal amount = wb.getAmount();
                BigDecimal price = wb.getCryptoCurrency().getPriceUsd() != null ? wb.getCryptoCurrency().getPriceUsd() : BigDecimal.ZERO;
                totalUsd = totalUsd.add(amount.multiply(price));
            }
        }

        PortfolioSnapshot snapshot = PortfolioSnapshot.builder()
                .user(user)
                .totalBalanceUsd(totalUsd)
                .recordedAt(LocalDateTime.now())
                .build();

        portfolioSnapshotRepository.save(snapshot);
    }
}