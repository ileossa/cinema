package com.cinema.cinema.infrastructure.adapter.in.web.dto;

public record RegisterRequest(
        String email,
        String fullName,
        String password
) {
}
