package com.cinema.cinema.infrastructure.adapter.in.web.dto;

public record AuthResponse(
        Long userId,
        String email,
        String fullName,
        String token
) {
}
