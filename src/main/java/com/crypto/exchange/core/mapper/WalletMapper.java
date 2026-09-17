package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.Wallet;
import com.crypto.exchange.core.entity.WalletBalance;
import com.crypto.exchange.web.dto.response.WalletBalanceResponseDto;
import com.crypto.exchange.web.dto.response.WalletResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WalletMapper {

    WalletResponseDto toResponseDto(Wallet entity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "cryptoId", source = "entity.cryptoCurrency.id")
    @Mapping(target = "externalId", source = "entity.cryptoCurrency.externalId")
    @Mapping(target = "symbol", source = "entity.cryptoCurrency.symbol")
    @Mapping(target = "cryptoName", source = "entity.cryptoCurrency.name")
    @Mapping(target = "amount", source = "entity.amount")
    @Mapping(target = "currentPriceUsd", source = "entity", qualifiedByName = "getCurrentPriceSafe")
    @Mapping(target = "totalValueUsd", expression = "java(calculateTotalValue(entity))")
    WalletBalanceResponseDto toBalanceResponseDto(WalletBalance entity);

    @Named("getCurrentPriceSafe")
    default BigDecimal getCurrentPriceSafe(WalletBalance entity) {
        if (entity == null || entity.getCryptoCurrency() == null || entity.getCryptoCurrency().getPriceUsd() == null) {
            return BigDecimal.ZERO;
        }
        return entity.getCryptoCurrency().getPriceUsd();
    }

    default BigDecimal calculateTotalValue(WalletBalance entity) {
        if (entity == null || entity.getAmount() == null || entity.getCryptoCurrency() == null || entity.getCryptoCurrency().getPriceUsd() == null) {
            return BigDecimal.ZERO;
        }
        return entity.getAmount().multiply(entity.getCryptoCurrency().getPriceUsd());
    }
}