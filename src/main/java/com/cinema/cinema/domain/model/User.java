package com.cinema.cinema.domain.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record User(
        Long id,
        String email,
        String fullName,
        Set<Role> roles,
        LocalDateTime createdAt
) {
    public User {
        if (null == email || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (null == fullName || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
        roles = roles != null ? Collections.unmodifiableSet(new HashSet<>(roles)) : Collections.emptySet();
    }

    public User(Long id, String email, String fullName, LocalDateTime createdAt) {
        this(id, email, fullName, Collections.emptySet(), createdAt);
    }

    public boolean hasRole(String roleName) {
        if (roleName == null || roles == null) {
            return false;
        }
        return roles.stream().anyMatch(r -> r.name().equalsIgnoreCase(roleName.trim()));
    }

    public User withRoles(Set<Role> newRoles) {
        return new User(id, email, fullName, newRoles, createdAt);
    }
}