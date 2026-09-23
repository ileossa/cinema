package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.Movie;
import com.cinema.cinema.domain.port.in.GetMoviesCurrentlyInTheaterUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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

    @Test
    @DisplayName("doit retourner la liste des films avec tous les champs mappés via l'api")
    void mustReturnTheListOfMoviesViaTheRESTEndpoint() throws Exception {
        Movie movie = new Movie(1L, "Dune", "Un film épique", 155, "dune.jpg", "http://minio/dune.jpg");
        given(getMoviesCurrentlyInTheaterUseCase.getMoviesCurrentlyInTheater()).willReturn(List.of(movie));

        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Dune"))
                .andExpect(jsonPath("$[0].description").value("Un film épique"))
                .andExpect(jsonPath("$[0].durationMinutes").value(155))
                .andExpect(jsonPath("$[0].posterUrl").value("http://minio/dune.jpg"));
    }

    @Test
    @DisplayName("doit retourner une liste vide lorsqu'aucun film n'est à l'affiche")
    void mustReturnEmptyListWhenNoMoviesAvailable() throws Exception {
        given(getMoviesCurrentlyInTheaterUseCase.getMoviesCurrentlyInTheater()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
