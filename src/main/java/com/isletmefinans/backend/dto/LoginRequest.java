package com.isletmefinans.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Kullanici adi zorunludur")
        String username,
        @NotBlank(message = "Sifre zorunludur")
        String password
) {
}
