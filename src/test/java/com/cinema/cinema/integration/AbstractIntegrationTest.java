package com.cinema.cinema.integration;

import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.MovieEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.ScreeningEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataBookingRepository;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataMovieRepository;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataScreeningRepository;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    protected SpringDataBookingRepository bookingRepository;

    @Autowired
    protected SpringDataScreeningRepository screeningRepository;

    @Autowired
    protected SpringDataMovieRepository movieRepository;

    @Autowired
    protected SpringDataUserRepository userRepository;

    @BeforeEach
    void setupBase() {
        bookingRepository.deleteAll();
        screeningRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }

    protected <T> T fromJson(MvcResult result, Class<T> clazz) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), clazz);
    }

    protected MovieEntity createAndSaveMovie(String title, String description, String synopsis, int durationMinutes, String posterFileName, String trailerUrl) {
        MovieEntity movie = new MovieEntity();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setSynopsis(synopsis);
        movie.setDurationMinutes(durationMinutes);
        movie.setPosterFileName(posterFileName);
        movie.setTrailerUrl(trailerUrl);
        return movieRepository.save(movie);
    }

    protected ScreeningEntity createAndSaveScreening(Long movieId, LocalDateTime startDateTime, String roomName, int availableSeats, BigDecimal unitPrice) {
        ScreeningEntity screening = new ScreeningEntity();
        screening.setMovieId(movieId);
        screening.setStartDateTime(startDateTime);
        screening.setRoomName(roomName);
        screening.setAvailableSeats(availableSeats);
        screening.setUnitPrice(unitPrice);
        return screeningRepository.save(screening);
    }

    protected UserEntity createAndSaveUser(String email, String fullName) {
        UserEntity user = new UserEntity(null, email, fullName, LocalDateTime.now());
        return userRepository.save(user);
    }
}
