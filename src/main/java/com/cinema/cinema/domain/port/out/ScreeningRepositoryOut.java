package com.cinema.cinema.domain.port.out;

import com.cinema.cinema.domain.model.Screening;

import java.util.List;
import java.util.Optional;

public interface ScreeningRepositoryOut {
    List<Screening> findByMovieId(Long movieId);
    Optional<Screening> findById(Long id);
    boolean reserveSeats(Long screeningId, int seatCount);
    void releaseSeats(Long screeningId, int seatCount);
}
