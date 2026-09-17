package com.cinema.cinema.domain.port.out;

import com.cinema.cinema.domain.model.Screening;

import java.util.List;

public interface ScreeningRepositoryOut {
    List<Screening> findByMovieId(Long movieId);
}
