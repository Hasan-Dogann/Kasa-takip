package com.isletmefinans.backend.dto;

import com.isletmefinans.backend.entity.CreditTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditTransactionResponse(
        Long id,
        String transactionType,
        BigDecimal amount,
        String description,
        LocalDateTime occurredAt,
        BigDecimal runningBalance
) {

    public static CreditTransactionResponse from(CreditTransaction transaction, BigDecimal runningBalance) {
        return new CreditTransactionResponse(
                transaction.getId(),
                transaction.getTransactionType().name(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getOccurredAt(),
                runningBalance
        );
    }
}
