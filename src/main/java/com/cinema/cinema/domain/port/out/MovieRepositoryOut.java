package com.cinema.cinema.domain.port.out;

import com.cinema.cinema.domain.model.Movie;

import java.util.List;
import java.util.Optional;

public interface MovieRepositoryOut {
    List<Movie> getMovies();

    Optional<Movie> findById(Long id);
}
