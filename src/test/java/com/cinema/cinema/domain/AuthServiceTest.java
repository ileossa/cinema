package com.cinema.cinema.domain;

import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.in.AuthUseCase;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;
import com.cinema.cinema.domain.service.AuthService;
import com.cinema.cinema.domain.service.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepositoryOut userRepositoryMock;
    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepositoryMock = mock(UserRepositoryOut.class);
        jwtTokenProvider = new JwtTokenProvider("test-secret-key-cinema-test-1234567890", 3600);
        authService = new AuthService(userRepositoryMock, jwtTokenProvider);
    }

    @Test
    @DisplayName("should login and return token when credentials are valid")
    void shouldLoginSuccessfully() {
        User user = new User(1L, "alice@cinema.fr", "Alice Dupont", LocalDateTime.now());
        when(userRepositoryMock.findByEmail("alice@cinema.fr")).thenReturn(Optional.of(user));

        AuthUseCase.AuthResult result = authService.login("alice@cinema.fr", "secret123");

        assertNotNull(result);
        assertEquals(1L, result.userId());
        assertEquals("alice@cinema.fr", result.email());
        assertEquals("Alice Dupont", result.fullName());
        assertNotNull(result.token());

        assertTrue(jwtTokenProvider.validateToken(result.token()));
        assertEquals(1L, jwtTokenProvider.getUserIdFromToken(result.token()));
        assertEquals("alice@cinema.fr", jwtTokenProvider.getEmailFromToken(result.token()));
        assertEquals("Alice Dupont", jwtTokenProvider.getFullNameFromToken(result.token()));
    }

    @Test
    @DisplayName("should throw when login with unknown email")
    void shouldThrowWhenLoginWithUnknownEmail() {
        when(userRepositoryMock.findByEmail("unknown@cinema.fr")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                authService.login("unknown@cinema.fr", "secret123")
        );
    }

    @Test
    @DisplayName("should register new user and return token")
    void shouldRegisterSuccessfully() {
        when(userRepositoryMock.findByEmail("bob@cinema.fr")).thenReturn(Optional.empty());
        when(userRepositoryMock.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return new User(2L, u.email(), u.fullName(), LocalDateTime.now());
        });

        AuthUseCase.AuthResult result = authService.register("bob@cinema.fr", "Bob Martin", "pwd123");

        assertNotNull(result);
        assertEquals(2L, result.userId());
        assertEquals("bob@cinema.fr", result.email());
        assertEquals("Bob Martin", result.fullName());
        assertNotNull(result.token());
        assertTrue(jwtTokenProvider.validateToken(result.token()));
    }

    @Test
    @DisplayName("should throw when registering already existing email")
    void shouldThrowWhenRegisteringExistingEmail() {
        User existing = new User(1L, "alice@cinema.fr", "Alice Dupont", LocalDateTime.now());
        when(userRepositoryMock.findByEmail("alice@cinema.fr")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () ->
                authService.register("alice@cinema.fr", "Alice Dupont", "pwd123")
        );
    }
}
