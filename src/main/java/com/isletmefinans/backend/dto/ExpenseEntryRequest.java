package com.isletmefinans.backend.dto;

import com.isletmefinans.backend.entity.ExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ExpenseEntryRequest(
        @NotNull(message = "Gider kategorisi zorunludur")
        ExpenseCategory category,
        @NotNull(message = "Gider tutari zorunludur")
        @DecimalMin(value = "0.01", message = "Gider tutari sifirdan buyuk olmalidir")
        BigDecimal amount,
        @Size(max = 500, message = "Gider aciklamasi en fazla 500 karakter olabilir")
        String description
) {
}
