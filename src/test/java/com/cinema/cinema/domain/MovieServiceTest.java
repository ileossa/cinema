package com.cinema.cinema.domain;

import com.cinema.cinema.domain.model.Movie;
import com.cinema.cinema.domain.port.out.MediaStoragePort;
import com.cinema.cinema.domain.port.out.MovieRepositoryPort;
import com.cinema.cinema.domain.service.MovieService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class) // permet de remplacer les _ du nom des tests par un espace
public class MovieServiceTest {

    @Test
    void should_return_the_movies_with_their_signed_poster_URLs(){
        // GIVEN
        MovieRepositoryPort repoMock = mock(MovieRepositoryPort.class);
        MediaStoragePort mediaStorageMock = mock(MediaStoragePort.class);

        Movie movie1 = new Movie(1L, "Inception", "Sci-Fi", 148, "inception.jpg", null);
        when(repoMock.getMovies()).thenReturn(List.of(movie1));
        when(mediaStorageMock.generateUrlAccesMedia("inception.jpg")).thenReturn("http://minio/signed/inception.jpg");

        MovieService movieService = new MovieService(repoMock, mediaStorageMock);

        // WHEN
        List<Movie> res = movieService.getMoviesCurrentlyInTheater();

        // THEN
        assertEquals(1, res.size());
        assertEquals("http://minio/signed/inception.jpg", res.get(0).posterUrl());
        verify(mediaStorageMock, times(1)).generateUrlAccesMedia("inception.jpg");
    }
}
