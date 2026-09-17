package com.cinema.cinema.infrastructure.adapter.out.persistence.repository;

import com.cinema.cinema.domain.model.Screening;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.MovieEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataMovieRepository extends JpaRepository<MovieEntity, Long> {
}
