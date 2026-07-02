package com.isletmefinans.backend.dto;

import com.isletmefinans.backend.entity.ExpenseEntry;

import java.math.BigDecimal;

public record ExpenseEntryResponse(
        Long id,
        String category,
        String categoryLabel,
        BigDecimal amount,
        String description
) {

    public static ExpenseEntryResponse from(ExpenseEntry entry) {
        return new ExpenseEntryResponse(
                entry.getId(),
                entry.getCategory().getValue(),
                entry.getCategory().getLabel(),
                entry.getAmount(),
                entry.getDescription()
        );
    }
}
