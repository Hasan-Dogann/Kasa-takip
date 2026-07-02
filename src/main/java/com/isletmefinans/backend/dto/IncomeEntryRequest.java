package com.isletmefinans.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record IncomeEntryRequest(
        @NotNull(message = "Gelir tutari zorunludur")
        @DecimalMin(value = "0.01", message = "Gelir tutari sifirdan buyuk olmalidir")
        BigDecimal amount,
        @Size(max = 500, message = "Gelir aciklamasi en fazla 500 karakter olabilir")
        String description
) {
}
