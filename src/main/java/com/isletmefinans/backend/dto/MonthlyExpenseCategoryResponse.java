package com.isletmefinans.backend.dto;

import java.math.BigDecimal;

public record MonthlyExpenseCategoryResponse(
        String category,
        String categoryLabel,
        BigDecimal totalExpense
) {
}
