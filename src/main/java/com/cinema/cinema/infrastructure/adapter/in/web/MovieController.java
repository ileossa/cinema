package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.port.in.GetMovieByIdUseCase;
import com.cinema.cinema.domain.port.in.GetMoviesCurrentlyInTheaterUseCase;
import com.cinema.cinema.infrastructure.adapter.in.web.dto.MovieResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

    private final GetMoviesCurrentlyInTheaterUseCase getMoviesCurrentlyInTheaterUseCase;
    private final GetMovieByIdUseCase getMovieByIdUseCase;

    public MovieController(
            GetMoviesCurrentlyInTheaterUseCase getMoviesCurrentlyInTheaterUseCase,
            GetMovieByIdUseCase getMovieByIdUseCase
    ) {
        this.getMoviesCurrentlyInTheaterUseCase = getMoviesCurrentlyInTheaterUseCase;
        this.getMovieByIdUseCase = getMovieByIdUseCase;
    }

    @GetMapping
    public List<MovieResponse> getMovies() {
        return getMoviesCurrentlyInTheaterUseCase.getMoviesCurrentlyInTheater().stream()
                .map(MovieResponse::fromDomain)
                .toList();
    }

    @GetMapping("/{id}")
    public MovieResponse getMovieById(@PathVariable("id") Long id) {
        return getMovieByIdUseCase.getMovieById(id)
                .map(MovieResponse::fromDomain)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found with id: " + id));
    }
}
