package com.cinema.cinema.domain.service;

import com.cinema.cinema.domain.model.Movie;
import com.cinema.cinema.domain.port.in.GetMovieByIdUseCase;
import com.cinema.cinema.domain.port.in.GetMoviesCurrentlyInTheaterUseCase;
import com.cinema.cinema.domain.port.out.MediaStorageOut;
import com.cinema.cinema.domain.port.out.MovieRepositoryOut;

import java.util.List;
import java.util.Optional;

public class MovieService implements GetMoviesCurrentlyInTheaterUseCase, GetMovieByIdUseCase {

    private final MovieRepositoryOut movieRepositoryOut;
    private final MediaStorageOut mediaStorageOut;

    public MovieService(MovieRepositoryOut movieRepositoryOut, MediaStorageOut mediaStorageOut) {
        this.movieRepositoryOut = movieRepositoryOut;
        this.mediaStorageOut = mediaStorageOut;
    }

    @Override
    public List<Movie> getMoviesCurrentlyInTheater() {
        return movieRepositoryOut.getMovies().stream().map(movie -> {
            if (movie.poster() != null && !movie.poster().isBlank() && mediaStorageOut != null) {
                String urlSigned = mediaStorageOut.generateUrlAccesMedia(movie.poster());
                return movie.withPosterUrl(urlSigned);
            }
            return movie;
        }).toList();
    }

    @Override
    public Optional<Movie> getMovieById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return movieRepositoryOut.findById(id).map(movie -> {
            if (movie.poster() != null && !movie.poster().isBlank() && mediaStorageOut != null) {
                String urlSigned = mediaStorageOut.generateUrlAccesMedia(movie.poster());
                return movie.withPosterUrl(urlSigned);
            }
            return movie;
        });
    }
}
