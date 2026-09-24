package com.cinema.cinema.infrastructure.adapter.out.persistence;

import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;

import java.util.Optional;

public class PostgresUserAdapter implements UserRepositoryOut {

    private final SpringDataUserRepository springDataUserRepository;

    public PostgresUserAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = new UserEntity(
                user.id(),
                user.email(),
                user.fullName(),
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
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getFullName(),
                entity.getCreatedAt()
        );
    }
}
