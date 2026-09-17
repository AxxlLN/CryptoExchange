package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.Transaction;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.dto.response.TransactionResponseDto.TransactionDirection;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {

    @Mapping(target = "fromWalletId", source = "entity.fromWallet.id")
    @Mapping(target = "toWalletId", source = "entity.toWallet.id")
    @Mapping(target = "fromCryptoSymbol", source = "entity.fromCrypto.symbol")
    @Mapping(target = "toCryptoSymbol", source = "entity.toCrypto.symbol")
    @Mapping(target = "direction", expression = "java(calculateDirection(entity, userId))")
    TransactionResponseDto toResponseDto(Transaction entity, @Context Long userId);

    default TransactionDirection calculateDirection(Transaction entity, Long userId) {
        boolean isFromMine = entity.getFromWallet() != null
                && entity.getFromWallet().getUser().getId().equals(userId);

        boolean isToMine = entity.getToWallet() != null
                && entity.getToWallet().getUser().getId().equals(userId);

        if (isFromMine && isToMine) {
            return TransactionDirection.SELF;
        } else if (isFromMine) {
            return TransactionDirection.OUTGOING;
        } else {
            return TransactionDirection.INCOMING;
        }
    }
}