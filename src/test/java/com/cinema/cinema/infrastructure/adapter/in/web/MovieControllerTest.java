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

@WebMvcTest(FilmController.class)
public class MovieControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMoviesCurrentlyInTheaterUseCase getMoviesCurrentlyInTheaterUseCase;

    @Test
    @DisplayName("doit retourner la liste des films via l'api")
    void mustReturnTheListOfMoviesViaTheRESTEndpoint() throws Exception {
        Movie movie = new Movie(1L, "Dune", "Un film épique", 155, "dune.jpg", "http://minio/dune.jpg");
        given(getMoviesCurrentlyInTheaterUseCase.getMoviesCurrentlyInTheater()).willReturn(List.of(movie));

        mockMvc.perform(get("/api/v1/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titre").value("Dune"))
                .andExpect(jsonPath("$[0].urlAffiche").value("http://minio/dune.jpg"));
    }
}
