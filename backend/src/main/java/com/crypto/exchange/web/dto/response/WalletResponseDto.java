package com.crypto.exchange.web.dto.response;

import java.util.List;

public record WalletResponseDto(
        Long id,
        String address,
        String name,
        Boolean isDefault,
        List<WalletBalanceResponseDto> balances
) {}