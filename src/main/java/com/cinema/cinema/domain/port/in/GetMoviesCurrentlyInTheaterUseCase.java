package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.Movie;
import java.util.List;

public interface GetMoviesCurrentlyInTheaterUseCase {
    List<Movie> getMoviesCurrentlyInTheater();
}