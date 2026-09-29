package com.cinema.cinema.infrastructure.adapter.out.persistence.repository;

import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataRoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(String name);
}
