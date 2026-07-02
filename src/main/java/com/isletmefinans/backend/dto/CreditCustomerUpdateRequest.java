package com.isletmefinans.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreditCustomerUpdateRequest(
        @NotBlank(message = "Ad soyad zorunludur")
        @Size(max = 160, message = "Ad soyad en fazla 160 karakter olabilir")
        String fullName,
        @NotBlank(message = "Telefon numarasi zorunludur")
        @Size(max = 40, message = "Telefon numarasi en fazla 40 karakter olabilir")
        String phoneNumber
) {
}
