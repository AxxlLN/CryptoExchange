package com.crypto.exchange.core.mapper;

import com.crypto.exchange.core.entity.CryptoCurrency;
import com.crypto.exchange.core.entity.CryptoPriceHistory;
import com.crypto.exchange.web.dto.response.CryptoCurrencyResponseDto;
import com.crypto.exchange.web.dto.response.CryptoPriceHistoryDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CryptoMapper {

    CryptoCurrencyResponseDto toResponseDto(CryptoCurrency entity);

    CryptoPriceHistoryDto toHistoryDto(CryptoPriceHistory entity);
}