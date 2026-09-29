package com.cinema.cinema.domain.service;

import com.cinema.cinema.domain.model.Role;
import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.in.AuthUseCase;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthService implements AuthUseCase {

    private final UserRepositoryOut userRepositoryOut;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepositoryOut userRepositoryOut, JwtTokenProvider jwtTokenProvider) {
        this.userRepositoryOut = userRepositoryOut;
        this.jwtTokenProvider = jwtTokenProvider != null ? jwtTokenProvider : new JwtTokenProvider();
    }

    @Override
    public AuthResult login(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        User user = userRepositoryOut.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        Set<String> roleNames = extractRoleNames(user);
        String token = jwtTokenProvider.generateToken(user.id(), user.email(), user.fullName(), roleNames);
        return new AuthResult(user.id(), user.email(), user.fullName(), token);
    }

    @Override
    public AuthResult register(String email, String fullName, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (userRepositoryOut.findByEmail(email.trim()).isPresent()) {
            throw new IllegalStateException("User already exists with email: " + email);
        }

        User newUser = new User(null, email.trim(), fullName.trim(), LocalDateTime.now());
        User savedUser = userRepositoryOut.save(newUser);

        Set<String> roleNames = extractRoleNames(savedUser);
        String token = jwtTokenProvider.generateToken(savedUser.id(), savedUser.email(), savedUser.fullName(), roleNames);
        return new AuthResult(savedUser.id(), savedUser.email(), savedUser.fullName(), token);
    }

    public JwtTokenProvider getJwtTokenProvider() {
        return jwtTokenProvider;
    }

    private Set<String> extractRoleNames(User user) {
        if (user == null || user.roles() == null || user.roles().isEmpty()) {
            return Collections.emptySet();
        }
        return user.roles().stream()
                .map(Role::name)
                .collect(Collectors.toSet());
    }
}
