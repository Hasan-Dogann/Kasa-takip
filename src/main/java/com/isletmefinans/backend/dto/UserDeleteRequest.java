package com.isletmefinans.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UserDeleteRequest(
        @NotBlank(message = "Ana admin sifresi zorunludur")
        String mainAdminPassword
) {
}
