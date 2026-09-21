package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.Movie;
import com.cinema.cinema.domain.port.in.GetMoviesCurrentlyInTheaterUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {
    private final GetMoviesCurrentlyInTheaterUseCase getMoviesCurrentlyInTheaterUseCase;

    public MovieController(GetMoviesCurrentlyInTheaterUseCase getMoviesCurrentlyInTheaterUseCase) {
        this.getMoviesCurrentlyInTheaterUseCase = getMoviesCurrentlyInTheaterUseCase;
    }

    @GetMapping
    public List<Movie> getMovies() {
        return getMoviesCurrentlyInTheaterUseCase.getMoviesCurrentlyInTheater();
    }
}
