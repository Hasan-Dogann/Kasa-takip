package com.isletmefinans.backend.dto;

public record LoginResponse(
        UserResponse user,
        String message
) {
}
