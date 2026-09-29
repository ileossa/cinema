package com.cinema.cinema.infrastructure.adapter.out.persistence;

import com.cinema.cinema.domain.model.Role;
import com.cinema.cinema.domain.port.out.RoleRepositoryOut;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataRoleRepository;

import java.util.List;
import java.util.Optional;

public class PostgresRoleAdapter implements RoleRepositoryOut {

    private final SpringDataRoleRepository springDataRoleRepository;

    public PostgresRoleAdapter(SpringDataRoleRepository springDataRoleRepository) {
        this.springDataRoleRepository = springDataRoleRepository;
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity = new RoleEntity(role.id(), role.name(), role.description());
        RoleEntity saved = springDataRoleRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return springDataRoleRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return springDataRoleRepository.findByName(name.trim().toUpperCase()).map(this::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return springDataRoleRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private Role toDomain(RoleEntity entity) {
        return new Role(entity.getId(), entity.getName(), entity.getDescription());
    }
}
