package com.cinema.cinema.domain;

import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;
import com.cinema.cinema.domain.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class UserServiceTest {

    private UserRepositoryOut userRepositoryMock;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepositoryMock = mock(UserRepositoryOut.class);
        userService = new UserService(userRepositoryMock);
    }

    @Test
    void should_return_user_profile_when_user_exists() {
        // GIVEN
        Long userId = 1L;
        User user = new User(userId, "john@example.com", "John Doe", LocalDateTime.now());
        when(userRepositoryMock.findById(userId)).thenReturn(Optional.of(user));

        // WHEN
        Optional<User> result = userService.getUserProfile(userId);

        // THEN
        assertTrue(result.isPresent());
        assertEquals("john@example.com", result.get().email());
        assertEquals("John Doe", result.get().fullName());
    }

    @Test
    void should_return_empty_when_user_not_found() {
        // GIVEN
        Long userId = 999L;
        when(userRepositoryMock.findById(userId)).thenReturn(Optional.empty());

        // WHEN
        Optional<User> result = userService.getUserProfile(userId);

        // THEN
        assertTrue(result.isEmpty());
    }
}
