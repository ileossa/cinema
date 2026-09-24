package com.cinema.cinema.infrastructure.adapter.out.persistence.repository;

import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.ScreeningEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataScreeningRepository extends JpaRepository<ScreeningEntity, Long> {
    List<ScreeningEntity> findByMovieId(Long movieId);

    @Modifying
    @Query("UPDATE ScreeningEntity s SET s.availableSeats = s.availableSeats - :seatCount WHERE s.id = :screeningId AND s.availableSeats >= :seatCount")
    int reserveSeats(@Param("screeningId") Long screeningId, @Param("seatCount") int seatCount);

    @Modifying
    @Query("UPDATE ScreeningEntity s SET s.availableSeats = s.availableSeats + :seatCount WHERE s.id = :screeningId")
    int releaseSeats(@Param("screeningId") Long screeningId, @Param("seatCount") int seatCount);
}