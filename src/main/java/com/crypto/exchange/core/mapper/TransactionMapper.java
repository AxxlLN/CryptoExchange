package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.Transaction;
import com.crypto.exchange.web.dto.response.TransactionResponseDto;
import com.crypto.exchange.web.dto.response.TransactionResponseDto.TransactionDirection;
import com.crypto.exchange.web.dto.response.TransferResponseDto;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {

    @Mapping(target = "fromWalletAddress", source = "entity.fromWallet.address")
    @Mapping(target = "toWalletAddress", source = "entity.toWallet.address")
    @Mapping(target = "fromCryptoSymbol", source = "entity.fromCrypto.symbol")
    @Mapping(target = "toCryptoSymbol", source = "entity.toCrypto.symbol")
    @Mapping(target = "direction", expression = "java(calculateDirection(entity, userId))")
    TransactionResponseDto toResponseDto(Transaction entity, @Context Long userId);

    @Mapping(target = "transactionId", source = "id")
    @Mapping(target = "senderAddress", source = "fromWallet.address")
    @Mapping(target = "recipientAddress", source = "toWallet.address")
    @Mapping(target = "cryptoSymbol", source = "fromCrypto.symbol")
    @Mapping(target = "amount", source = "fromAmount")
    @Mapping(target = "timestamp", source = "createdAt")
    TransferResponseDto toTransferResponseDto(Transaction entity);

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