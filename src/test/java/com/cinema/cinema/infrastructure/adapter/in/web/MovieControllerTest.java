package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.Movie;
import com.cinema.cinema.domain.port.in.GetMovieByIdUseCase;
import com.cinema.cinema.domain.port.in.GetMoviesCurrentlyInTheaterUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieController.class)
class MovieControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMoviesCurrentlyInTheaterUseCase getMoviesCurrentlyInTheaterUseCase;

    @MockitoBean
    private GetMovieByIdUseCase getMovieByIdUseCase;

    @Test
    @DisplayName("doit retourner la liste des films avec tous les champs mappés via l'api")
    void mustReturnTheListOfMoviesViaTheRESTEndpoint() throws Exception {
        Movie movie = new Movie(1L, "Dune", "Un film épique", "Synopsis complet de Dune", 155, "dune.jpg", "http://minio/dune.jpg", "https://youtube.com/watch?v=dune");
        given(getMoviesCurrentlyInTheaterUseCase.getMoviesCurrentlyInTheater()).willReturn(List.of(movie));

        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Dune"))
                .andExpect(jsonPath("$[0].description").value("Un film épique"))
                .andExpect(jsonPath("$[0].synopsis").value("Synopsis complet de Dune"))
                .andExpect(jsonPath("$[0].durationMinutes").value(155))
                .andExpect(jsonPath("$[0].posterUrl").value("http://minio/dune.jpg"))
                .andExpect(jsonPath("$[0].trailerUrl").value("https://youtube.com/watch?v=dune"));
    }

    @Test
    @DisplayName("doit retourner une liste vide lorsqu'aucun film n'est à l'affiche")
    void mustReturnEmptyListWhenNoMoviesAvailable() throws Exception {
        given(getMoviesCurrentlyInTheaterUseCase.getMoviesCurrentlyInTheater()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("doit retourner un film par son id")
    void shouldReturnMovieById() throws Exception {
        Movie movie = new Movie(1L, "Dune", "Un film épique", "Synopsis complet de Dune", 155, "dune.jpg", "http://minio/dune.jpg", "https://youtube.com/watch?v=dune");
        given(getMovieByIdUseCase.getMovieById(1L)).willReturn(Optional.of(movie));

        mockMvc.perform(get("/api/v1/movies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Dune"))
                .andExpect(jsonPath("$.synopsis").value("Synopsis complet de Dune"))
                .andExpect(jsonPath("$.trailerUrl").value("https://youtube.com/watch?v=dune"));
    }

    @Test
    @DisplayName("doit retourner 404 quand le film n'existe pas")
    void shouldReturn404WhenMovieNotFound() throws Exception {
        given(getMovieByIdUseCase.getMovieById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/movies/999"))
                .andExpect(status().isNotFound());
    }
}
