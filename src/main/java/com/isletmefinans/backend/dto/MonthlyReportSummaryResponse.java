package com.isletmefinans.backend.dto;

import java.math.BigDecimal;

public record MonthlyReportSummaryResponse(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal totalRemaining,
        int recordCount
) {
}
