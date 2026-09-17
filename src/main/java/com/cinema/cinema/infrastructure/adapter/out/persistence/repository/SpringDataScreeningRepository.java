package com.cinema.cinema.infrastructure.adapter.out.persistence.repository;

import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.ScreeningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SpringDataScreeningRepository extends JpaRepository<ScreeningEntity, Long> {
    List<ScreeningEntity> findByMovieId(Long movieId);
}