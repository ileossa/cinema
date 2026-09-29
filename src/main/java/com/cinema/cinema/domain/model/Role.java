package com.cinema.cinema.domain.model;

public record Role(
        Long id,
        String name,
        String description
) {
    public Role {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name cannot be empty");
        }
        name = name.trim().toUpperCase();
    }

    public Role(Long id, String name) {
        this(id, name, null);
    }
}
