package com.cinema.cinema.domain.service;

import com.cinema.cinema.domain.model.Role;
import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.in.ChangeUserRoleUseCase;
import com.cinema.cinema.domain.port.in.GetUserProfileUseCase;
import com.cinema.cinema.domain.port.out.RoleRepositoryOut;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class UserService implements GetUserProfileUseCase, ChangeUserRoleUseCase {

    private final UserRepositoryOut userRepositoryOut;
    private final RoleRepositoryOut roleRepositoryOut;

    public UserService(UserRepositoryOut userRepositoryOut, RoleRepositoryOut roleRepositoryOut) {
        this.userRepositoryOut = userRepositoryOut;
        this.roleRepositoryOut = roleRepositoryOut;
    }

    public UserService(UserRepositoryOut userRepositoryOut) {
        this(userRepositoryOut, null);
    }

    @Override
    public Optional<User> getUserProfile(Long userId) {
        return userRepositoryOut.findById(userId);
    }

    @Override
    public User assignRole(Long userId, String roleName) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role name cannot be empty");
        }

        User user = userRepositoryOut.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Role role;
        if (roleRepositoryOut != null) {
            role = roleRepositoryOut.findByName(roleName.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        } else {
            role = new Role(null, roleName.trim());
        }

        Set<Role> updatedRoles = new HashSet<>(user.roles());
        updatedRoles.add(role);
        User updatedUser = user.withRoles(updatedRoles);
        return userRepositoryOut.save(updatedUser);
    }
}
