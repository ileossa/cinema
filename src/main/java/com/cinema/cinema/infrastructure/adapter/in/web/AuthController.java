package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.port.in.AuthUseCase;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.AuthResponse;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.LoginRequest;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody(required = false) LoginRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }
        try {
            AuthUseCase.AuthResult result = authUseCase.login(request.email(), request.password());
            return new AuthResponse(result.userId(), result.email(), result.fullName(), result.token());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody(required = false) RegisterRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.fullName() == null || request.fullName().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email, full name and password are required");
        }
        try {
            AuthUseCase.AuthResult result = authUseCase.register(request.email(), request.fullName(), request.password());
            return new AuthResponse(result.userId(), result.email(), result.fullName(), result.token());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
