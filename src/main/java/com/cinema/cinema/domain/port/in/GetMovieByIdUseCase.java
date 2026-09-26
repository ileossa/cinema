package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.Movie;

import java.util.Optional;

public interface GetMovieByIdUseCase {
    Optional<Movie> getMovieById(Long id);
}
