package com.cinema.cinema.infrastructure.adapter.in.web.dto;

import com.cinema.cinema.domain.model.Role;
import com.cinema.cinema.domain.model.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Set<String> roles,
        LocalDateTime createdAt
) {
    public UserResponse(Long id, String email, String fullName, LocalDateTime createdAt) {
        this(id, email, fullName, Collections.emptySet(), createdAt);
    }

    public static UserResponse fromDomain(User user) {
        Set<String> roleNames = user.roles() != null
                ? user.roles().stream().map(Role::name).collect(Collectors.toSet())
                : Collections.emptySet();
        return new UserResponse(
                user.id(),
                user.email(),
                user.fullName(),
                roleNames,
                user.createdAt()
        );
    }
}
