package com.cinema.cinema.domain.port.out;

import com.cinema.cinema.domain.model.User;

import java.util.Optional;

public interface UserRepositoryOut {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
}
