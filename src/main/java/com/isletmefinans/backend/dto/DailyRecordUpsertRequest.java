package com.isletmefinans.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record DailyRecordUpsertRequest(
        @NotNull(message = "Kayit tarihi zorunludur")
        LocalDate recordDate,
        @NotNull(message = "Gelir listesi zorunludur")
        List<@Valid IncomeEntryRequest> incomeItems,
        @NotNull(message = "Gider listesi zorunludur")
        List<@Valid ExpenseEntryRequest> expenseItems
) {
}
