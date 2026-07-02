package com.isletmefinans.backend.dto;

import java.util.List;

public record MonthlyReportResponse(
        MonthlyReportSummaryResponse summary,
        List<MonthlyExpenseCategoryResponse> expenseByCategory,
        List<MonthlyReportDayResponse> days
) {
}
