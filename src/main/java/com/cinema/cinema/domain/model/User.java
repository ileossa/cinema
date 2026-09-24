package com.cinema.cinema.domain.model;

import java.time.LocalDateTime;

public record User(
        Long id,
        String email,
        String fullName,
        LocalDateTime createdAt
) {
    public User {
        if (null == email || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (null == fullName || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
    }
}