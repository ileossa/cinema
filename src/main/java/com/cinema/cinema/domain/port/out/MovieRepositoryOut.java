package com.cinema.cinema.domain.port.out;

import com.cinema.cinema.domain.model.Movie;

import java.util.List;

public interface MovieRepositoryOut {
    List<Movie> getMovies();
}
