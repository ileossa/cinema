package com.cinema.cinema.infrastructure.adapter.in.web;

import com.cinema.cinema.domain.model.Screening;
import com.cinema.cinema.domain.port.in.GetMovieShowingsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScreeningController.class)
class ScreeningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMovieShowingsUseCase getMovieShowingsUseCase;

    @Test
    @DisplayName("doit retourner la liste des séances pour un film donné")
    void shouldReturnScreeningsForGivenMovieId() throws Exception {
        Long movieId = 1L;
        LocalDateTime screeningDateTime = LocalDateTime.of(2026, 10, 15, 20, 0);
        Screening screening = new Screening(100L, movieId, screeningDateTime, "Salle 1", 50, new BigDecimal("12.50"));

        given(getMovieShowingsUseCase.getMovieShowings(movieId)).willReturn(List.of(screening));

        mockMvc.perform(get("/api/v1/screenings").param("movieId", String.valueOf(movieId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].movieId").value(1))
                .andExpect(jsonPath("$[0].screeningDateTime").value("2026-10-15T20:00:00"))
                .andExpect(jsonPath("$[0].room").value("Salle 1"))
                .andExpect(jsonPath("$[0].availableSeats").value(50))
                .andExpect(jsonPath("$[0].price").value(12.50));
    }

    @Test
    @DisplayName("doit retourner une liste vide lorsqu'il n'y a pas de séance pour le film")
    void shouldReturnEmptyListWhenNoScreeningsForMovie() throws Exception {
        Long movieId = 2L;
        given(getMovieShowingsUseCase.getMovieShowings(movieId)).willReturn(List.of());

        mockMvc.perform(get("/api/v1/screenings").param("movieId", String.valueOf(movieId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("doit retourner 400 Bad Request lorsque le paramètre movieId est manquant")
    void shouldReturnBadRequestWhenMovieIdIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/screenings"))
                .andExpect(status().isBadRequest());
    }
}
