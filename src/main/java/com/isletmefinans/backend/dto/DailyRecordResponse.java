package com.isletmefinans.backend.dto;

import com.isletmefinans.backend.entity.DailyRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.isletmefinans.backend.util.AmountUtils.ZERO;

public record DailyRecordResponse(
        Long id,
        LocalDate recordDate,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal remainingBalance,
        List<IncomeEntryResponse> incomeItems,
        List<ExpenseEntryResponse> expenseItems,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DailyRecordResponse from(DailyRecord record) {
        return new DailyRecordResponse(
                record.getId(),
                record.getRecordDate(),
                record.getTotalIncome(),
                record.getTotalExpense(),
                record.getRemainingBalance(),
                record.getIncomeEntries().stream()
                        .map(IncomeEntryResponse::from)
                        .toList(),
                record.getExpenseEntries().stream()
                        .map(ExpenseEntryResponse::from)
                        .toList(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }

    public static DailyRecordResponse empty(LocalDate date) {
        return new DailyRecordResponse(
                null,
                date,
                ZERO,
                ZERO,
                ZERO,
                List.of(),
                List.of(),
                null,
                null
        );
    }
}
