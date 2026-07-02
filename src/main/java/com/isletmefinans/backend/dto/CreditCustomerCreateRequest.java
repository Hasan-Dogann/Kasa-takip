package com.isletmefinans.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditCustomerCreateRequest(
        @NotBlank(message = "Ad soyad zorunludur")
        @Size(max = 160, message = "Ad soyad en fazla 160 karakter olabilir")
        String fullName,
        @NotBlank(message = "Telefon numarasi zorunludur")
        @Size(max = 40, message = "Telefon numarasi en fazla 40 karakter olabilir")
        String phoneNumber,
        @NotNull(message = "Borclandirma tutari zorunludur")
        @DecimalMin(value = "0.01", message = "Borclandirma tutari sifirdan buyuk olmalidir")
        BigDecimal amount,
        @Size(max = 600, message = "Aciklama en fazla 600 karakter olabilir")
        String description,
        LocalDateTime occurredAt
) {
}
