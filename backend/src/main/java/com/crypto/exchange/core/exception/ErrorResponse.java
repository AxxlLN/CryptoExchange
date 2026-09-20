package com.crypto.exchange.core.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

public record ErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        OffsetDateTime timestamp,
        int status,
        String error,
        String message
) {}