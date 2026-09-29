package com.cinema.cinema.infrastructure.adapter.out.persistence;

import com.cinema.cinema.domain.model.Role;
import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PostgresUserAdapter implements UserRepositoryOut {

    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataRoleRepository springDataRoleRepository;

    public PostgresUserAdapter(SpringDataUserRepository springDataUserRepository, SpringDataRoleRepository springDataRoleRepository) {
        this.springDataUserRepository = springDataUserRepository;
        this.springDataRoleRepository = springDataRoleRepository;
    }

    public PostgresUserAdapter(SpringDataUserRepository springDataUserRepository) {
        this(springDataUserRepository, null);
    }

    @Override
    public User save(User user) {
        Set<RoleEntity> roleEntities = new HashSet<>();
        if (user.roles() != null && !user.roles().isEmpty()) {
            for (Role role : user.roles()) {
                if (springDataRoleRepository != null) {
                    RoleEntity roleEntity = springDataRoleRepository.findByName(role.name())
                            .orElseGet(() -> springDataRoleRepository.save(new RoleEntity(role.id(), role.name(), role.description())));
                    roleEntities.add(roleEntity);
                } else {
                    roleEntities.add(new RoleEntity(role.id(), role.name(), role.description()));
                }
            }
        } else if (springDataRoleRepository != null) {
            springDataRoleRepository.findByName("CLIENT").ifPresent(roleEntities::add);
        }

        UserEntity entity = new UserEntity(
                user.id(),
                user.email(),
                user.fullName(),
                roleEntities,
                user.createdAt()
        );
        UserEntity saved = springDataUserRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return springDataUserRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email).map(this::toDomain);
    }

    private User toDomain(UserEntity entity) {
        Set<Role> roles = entity.getRoles() != null
                ? entity.getRoles().stream()
                .map(r -> new Role(r.getId(), r.getName(), r.getDescription()))
                .collect(Collectors.toSet())
                : Set.of();
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getFullName(),
                roles,
                entity.getCreatedAt()
        );
    }
}
