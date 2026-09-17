package com.cinema.cinema.infrastructure.config;

import com.cinema.cinema.domain.port.out.MediaStoragePort;
import com.cinema.cinema.domain.port.out.MovieRepositoryPort;
import com.cinema.cinema.domain.port.out.ScreeningRepositoryOut;
import com.cinema.cinema.domain.service.MovieService;
import com.cinema.cinema.domain.service.ScreeningService;
import com.cinema.cinema.infrastructure.adapter.out.persistence.PostgresMovieAdapter;
import com.cinema.cinema.infrastructure.adapter.out.persistence.PostgresScreeningAdapter;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataMovieRepository;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataScreeningRepository;
import com.cinema.cinema.infrastructure.adapter.out.storage.RustFsStorageAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

// Class qui permet de centraliser les beans de l'application.
// Il est aussi possible de les déclarer dans les classes qui les utilisent (@component/@service/....)
@Configuration
public class ApplicationConfig {

    @Bean
    public MovieRepositoryPort movieRepositoryPort(SpringDataMovieRepository springDataMovieRepository) {
        return new PostgresMovieAdapter(springDataMovieRepository);
    }

    @Bean
    public MediaStoragePort mediaStoragePort(S3Presigner s3Presigner, @Value("${rustfs.bucket-name}") String bucketName) {
        return new RustFsStorageAdapter(s3Presigner, bucketName);
    }

    @Bean
    public MovieService movieService(MovieRepositoryPort movieRepositoryPort, MediaStoragePort mediaStoragePort) {
        return new MovieService(movieRepositoryPort, mediaStoragePort);
    }

    @Bean
    public ScreeningRepositoryOut screeningRepositoryOut(SpringDataScreeningRepository springDataScreeningRepository) {
        return new PostgresScreeningAdapter(springDataScreeningRepository);
    }

    @Bean
    public ScreeningService screeningService(ScreeningRepositoryOut screeningRepositoryOut) {
        return new ScreeningService(screeningRepositoryOut);
    }
}
