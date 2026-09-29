package com.cinema.cinema.infrastructure.adapter.out.persistence.repository;

import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.MovieEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataMovieRepository extends JpaRepository<MovieEntity, Long> {
    List<MovieEntity> findByIsPublishedTrue();

    Optional<MovieEntity> findByIdAndIsPublishedTrue(Long id);
}
