package com.isletmefinans.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditDebtRequest(
        @NotNull(message = "Borclandirma tutari zorunludur")
        @DecimalMin(value = "0.01", message = "Borclandirma tutari sifirdan buyuk olmalidir")
        BigDecimal amount,
        @Size(max = 600, message = "Aciklama en fazla 600 karakter olabilir")
        String description,
        LocalDateTime occurredAt
) {
}
