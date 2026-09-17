//package com.crypto.exchange.core.service;
//
//import com.crypto.exchange.core.entity.CryptoCurrency;
//import com.crypto.exchange.core.entity.CryptoPriceHistory;
//import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
//import com.crypto.exchange.core.repository.CryptoPriceHistoryRepository;
//import com.crypto.exchange.integration.coinlore.client.CoinLoreApiClient;
//import com.crypto.exchange.integration.coinlore.dto.CoinLoreResponseDto;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class CryptoInitializerService {
//
//    private final CryptoCurrencyRepository cryptoRepository;
//    private final CryptoPriceHistoryRepository priceHistoryRepository;
//    private final CoinLoreApiClient coinLoreApiClient;
//
//    @Value("${app.crypto.target-symbols:BTC,ETH,USDT,BNB,SOL,XRP,ADA,DOGE}")
//    private Set<String> targetSymbols;
//
//    /**
//     * Выполняется сразу при старте (initialDelayString = "0") и далее по расписанию.
//     */
//    @Scheduled(
//            fixedRateString = "${app.crypto.update-interval-ms:300000}",
//            initialDelayString = "${app.crypto.initial-delay-ms:0}"
//    )
//    public void scheduledPriceUpdate() {
//        log.info("Starting crypto price synchronization...");
//        syncPrices();
//    }
//
//    @Transactional
//    public void syncPrices() {
//        coinLoreApiClient.fetchLatestCryptoPrices()
//                .map(CoinLoreResponseDto::data)
//                .ifPresent(this::processAndSaveBatch);
//    }
//
//    public void processAndSaveBatch(List<CoinLoreResponseDto.CoinLoreCryptoData> apiData) {
//        List<CoinLoreResponseDto.CoinLoreCryptoData> filteredData = apiData.stream()
//                .filter(data -> data.symbol() != null && targetSymbols.contains(data.symbol().toUpperCase()))
//                .toList();
//
//        if (filteredData.isEmpty()) {
//            return;
//        }
//
//        Set<String> symbolsToFetch = filteredData.stream()
//                .map(d -> d.symbol().toUpperCase())
//                .collect(Collectors.toSet());
//
//        Map<String, CryptoCurrency> existingCryptos = cryptoRepository.findAllBySymbolIn(symbolsToFetch).stream()
//                .collect(Collectors.toMap(CryptoCurrency::getSymbol, Function.identity()));
//
//        List<CryptoCurrency> cryptosToSave = new ArrayList<>();
//        List<CryptoPriceHistory> historyToSave = new ArrayList<>();
//
//        for (var coinData : filteredData) {
//            String symbol = coinData.symbol().toUpperCase();
//            BigDecimal price = new BigDecimal(coinData.priceUsd()).setScale(4, RoundingMode.HALF_UP);
//
//            CryptoCurrency crypto = existingCryptos.get(symbol);
//
//            if (crypto != null) {
//                crypto.setPriceUsd(price);
//            } else {
//                crypto = CryptoCurrency.builder()
//                        .symbol(symbol)
//                        .name(coinData.name())
//                        .priceUsd(price)
//                        .build();
//            }
//            cryptosToSave.add(crypto);
//        }
//
//        List<CryptoCurrency> savedCryptos = cryptoRepository.saveAll(cryptosToSave);
//
//        for (CryptoCurrency crypto : savedCryptos) {
//            historyToSave.add(CryptoPriceHistory.builder()
//                    .cryptoCurrency(crypto)
//                    .priceUsd(crypto.getPriceUsd())
//                    .build());
//        }
//
//        priceHistoryRepository.saveAll(historyToSave);
//        log.info("Success! Updated {} cryptocurrencies", savedCryptos.size());
//    }
//}
package com.crypto.exchange.core.service;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.CryptoPriceHistory;
import com.crypto.exchange.core.repository.CryptoCurrencyRepository;
import com.crypto.exchange.core.repository.CryptoPriceHistoryRepository;
import com.crypto.exchange.integration.client.CoinLoreApiClient;
import com.crypto.exchange.integration.dto.CoinLoreResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoInitializerService {

    private final CryptoCurrencyRepository cryptoRepository;
    private final CryptoPriceHistoryRepository priceHistoryRepository;
    private final CoinLoreApiClient coinLoreApiClient;

    @Value("${app.crypto.target-symbols:BTC,ETH,USDT,BNB,SOL,XRP,ADA,DOGE}")
    private Set<String> targetSymbols;

    /**
     * Выполняется сразу при старте (initialDelayString = "0") и далее по расписанию.
     */
    @Scheduled(
            fixedRateString = "${app.crypto.update-interval-ms:300000}",
            initialDelayString = "${app.crypto.initial-delay-ms:0}"
    )
    public void scheduledPriceUpdate() {
        log.info("Starting crypto price synchronization...");
        syncPrices();
    }

    @Transactional
    @CacheEvict(value = "cryptoPrices", allEntries = true)
    public void syncPrices() {
        coinLoreApiClient.fetchLatestCryptoPrices()
                .map(CoinLoreResponseDto::data)
                .ifPresent(this::processAndSaveBatch);
    }

    public void processAndSaveBatch(List<CoinLoreResponseDto.CoinLoreCryptoData> apiData) {
        List<CoinLoreResponseDto.CoinLoreCryptoData> filteredData = apiData.stream()
                .filter(data -> data.symbol() != null && targetSymbols.contains(data.symbol().toUpperCase()))
                .toList();

        if (filteredData.isEmpty()) {
            log.warn("No matching cryptocurrency symbols found in API response.");
            return;
        }

        Set<String> externalIds = filteredData.stream()
                .map(CoinLoreResponseDto.CoinLoreCryptoData::id)
                .collect(Collectors.toSet());

        Set<String> symbols = filteredData.stream()
                .map(d -> d.symbol().toUpperCase())
                .collect(Collectors.toSet());

        Map<String, CryptoCurrency> existingByExternalId = cryptoRepository.findAllByExternalIdIn(externalIds).stream()
                .collect(Collectors.toMap(CryptoCurrency::getExternalId, Function.identity()));

        Map<String, CryptoCurrency> existingBySymbol = cryptoRepository.findAllBySymbolIn(symbols).stream()
                .collect(Collectors.toMap(CryptoCurrency::getSymbol, Function.identity()));

        List<CryptoCurrency> cryptosToSave = new ArrayList<>();

        for (var coinData : filteredData) {
            String symbol = coinData.symbol().toUpperCase();
            String coinLoreId = coinData.id();
            BigDecimal price = new BigDecimal(coinData.priceUsd()).setScale(4, RoundingMode.HALF_UP);

            CryptoCurrency crypto = existingByExternalId.get(coinLoreId);
            if (crypto == null) {
                crypto = existingBySymbol.get(symbol);
            }

            if (crypto != null) {
                crypto.setExternalId(coinLoreId);
                crypto.setPriceUsd(price);
            } else {
                crypto = CryptoCurrency.builder()
                        .externalId(coinLoreId)
                        .symbol(symbol)
                        .name(coinData.name())
                        .priceUsd(price)
                        .build();
            }
            cryptosToSave.add(crypto);
        }

        List<CryptoCurrency> savedCryptos = cryptoRepository.saveAll(cryptosToSave);

        List<CryptoPriceHistory> historyToSave = savedCryptos.stream()
                .map(crypto -> CryptoPriceHistory.builder()
                        .cryptoCurrency(crypto)
                        .priceUsd(crypto.getPriceUsd())
                        .build())
                .toList();

        priceHistoryRepository.saveAll(historyToSave);
        log.info("Success! Updated and mapped {} cryptocurrencies with CoinLore IDs", savedCryptos.size());
    }
}