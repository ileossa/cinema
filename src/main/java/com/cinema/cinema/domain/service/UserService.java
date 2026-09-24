package com.cinema.cinema.domain.service;

import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.in.GetUserProfileUseCase;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;

import java.util.Optional;

public class UserService implements GetUserProfileUseCase {

    private final UserRepositoryOut userRepositoryOut;

    public UserService(UserRepositoryOut userRepositoryOut) {
        this.userRepositoryOut = userRepositoryOut;
    }

    @Override
    public Optional<User> getUserProfile(Long userId) {
        return userRepositoryOut.findById(userId);
    }
}
