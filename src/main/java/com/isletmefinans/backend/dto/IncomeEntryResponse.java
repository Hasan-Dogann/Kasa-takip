package com.isletmefinans.backend.dto;

import com.isletmefinans.backend.entity.IncomeEntrySourceType;
import com.isletmefinans.backend.entity.IncomeEntry;

import java.math.BigDecimal;

public record IncomeEntryResponse(
        Long id,
        BigDecimal amount,
        String description,
        String sourceType,
        boolean systemGenerated
) {

    public static IncomeEntryResponse from(IncomeEntry entry) {
        IncomeEntrySourceType sourceType = entry.getSourceType() != null
                ? entry.getSourceType()
                : IncomeEntrySourceType.MANUAL;

        return new IncomeEntryResponse(
                entry.getId(),
                entry.getAmount(),
                entry.getDescription(),
                sourceType.name(),
                sourceType != IncomeEntrySourceType.MANUAL
        );
    }
}
