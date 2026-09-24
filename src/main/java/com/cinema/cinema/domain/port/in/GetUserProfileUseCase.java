package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.User;

import java.util.Optional;

public interface GetUserProfileUseCase {
    Optional<User> getUserProfile(Long userId);
}
