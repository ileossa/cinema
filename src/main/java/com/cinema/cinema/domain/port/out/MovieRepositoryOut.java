package com.cinema.cinema.domain.port.out;

import com.cinema.cinema.domain.model.Movie;

import java.util.List;
import java.util.Optional;

public interface MovieRepositoryOut {
    List<Movie> getMovies();

    List<Movie> findAll();

    List<Movie> findAllPublished();

    Optional<Movie> findById(Long id);

    Optional<Movie> findPublishedById(Long id);

    Movie save(Movie movie);
}
