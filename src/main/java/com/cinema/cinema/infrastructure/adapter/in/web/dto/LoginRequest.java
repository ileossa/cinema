package com.cinema.cinema.infrastructure.adapter.in.web.dto;

public record LoginRequest(
        String email,
        String password
) {
}
