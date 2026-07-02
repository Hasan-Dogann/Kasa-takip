package com.isletmefinans.backend.dto;

import com.isletmefinans.backend.entity.AppUser;

public record UserResponse(
        Long id,
        String username,
        String role
) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }
}
