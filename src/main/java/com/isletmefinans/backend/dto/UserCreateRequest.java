package com.isletmefinans.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "Kullanici adi zorunludur")
        @Size(max = 80, message = "Kullanici adi en fazla 80 karakter olabilir")
        String username,
        @NotBlank(message = "Sifre zorunludur")
        @Size(min = 6, message = "Sifre en az 6 karakter olmalidir")
        String password,
        @NotBlank(message = "Ana admin sifresi zorunludur")
        String mainAdminPassword
) {
}
