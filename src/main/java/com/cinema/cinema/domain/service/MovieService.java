package com.cinema.cinema.domain.service;

import com.cinema.cinema.domain.model.Movie;
import com.cinema.cinema.domain.port.in.GetMoviesCurrentlyInTheaterUseCase;
import com.cinema.cinema.domain.port.out.MediaStoragePort;
import com.cinema.cinema.domain.port.out.MovieRepositoryPort;

import java.util.List;

public class MovieService implements GetMoviesCurrentlyInTheaterUseCase {

    private final MovieRepositoryPort movieRepositoryPort;
    private final MediaStoragePort mediaStoragePort;

    public MovieService(MovieRepositoryPort movieRepositoryPort, MediaStoragePort mediaStoragePort) {
        this.movieRepositoryPort = movieRepositoryPort;
        this.mediaStoragePort = mediaStoragePort;
    }

    @Override
    public List<Movie> getMoviesCurrentlyInTheater() {
        return movieRepositoryPort.getMovies().stream().map(movie -> {
            if( movie.poster() != null ) {
                String urlSigned = mediaStoragePort.generateUrlAccesMedia(movie.poster());
                return movie.withUrlPoster(urlSigned);
            }
            return movie;
        }).toList();
    }
}
