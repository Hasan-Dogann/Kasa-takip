package com.isletmefinans.backend.dto;

import com.isletmefinans.backend.entity.DailyRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.isletmefinans.backend.util.AmountUtils.ZERO;

public record MonthlyReportDayResponse(
        LocalDate recordDate,
        boolean hasRecord,
        Long recordId,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal remainingBalance
) {

    public static MonthlyReportDayResponse from(DailyRecord record) {
        return new MonthlyReportDayResponse(
                record.getRecordDate(),
                true,
                record.getId(),
                record.getTotalIncome(),
                record.getTotalExpense(),
                record.getRemainingBalance()
        );
    }

    public static MonthlyReportDayResponse empty(LocalDate date) {
        return new MonthlyReportDayResponse(
                date,
                false,
                null,
                ZERO,
                ZERO,
                ZERO
        );
    }
}
