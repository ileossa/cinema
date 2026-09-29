package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.User;

public interface ChangeUserRoleUseCase {
    User assignRole(Long userId, String roleName);
}
