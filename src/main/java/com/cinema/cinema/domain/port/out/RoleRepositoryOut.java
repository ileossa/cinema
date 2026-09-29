package com.cinema.cinema.domain.port.out;

import com.cinema.cinema.domain.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepositoryOut {
    Role save(Role role);

    Optional<Role> findById(Long id);

    Optional<Role> findByName(String name);

    List<Role> findAll();
}
