package com.cinema.cinema.infrastructure.config;

import com.cinema.cinema.domain.port.out.*;
import com.cinema.cinema.domain.service.*;
import com.cinema.cinema.infrastructure.adapter.out.notification.EmailNotificationAdapter;
import com.cinema.cinema.infrastructure.adapter.out.payment.StripePaymentAdapter;
import com.cinema.cinema.infrastructure.adapter.out.persistence.PostgresBookingAdapter;
import com.cinema.cinema.infrastructure.adapter.out.persistence.PostgresMovieAdapter;
import com.cinema.cinema.infrastructure.adapter.out.persistence.PostgresScreeningAdapter;
import com.cinema.cinema.infrastructure.adapter.out.persistence.PostgresUserAdapter;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataBookingRepository;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataMovieRepository;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataScreeningRepository;
import com.cinema.cinema.infrastructure.adapter.out.persistence.repository.SpringDataUserRepository;
import com.cinema.cinema.infrastructure.adapter.out.qrcode.ZxingQrCodeAdapter;
import com.cinema.cinema.infrastructure.adapter.out.storage.RustFsStorageAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

// Class qui permet de centraliser les beans de l'application.
// Il est aussi possible de les déclarer dans les classes qui les utilisent (@component/@service/....)
@Configuration
@EnableScheduling
public class ApplicationConfig {

    @Bean
    public MovieRepositoryOut movieRepositoryPort(SpringDataMovieRepository springDataMovieRepository) {
        return new PostgresMovieAdapter(springDataMovieRepository);
    }

    @Bean
    public MediaStorageOut mediaStoragePort(S3Presigner s3Presigner, @Value("${rustfs.bucket-name}") String bucketName) {
        return new RustFsStorageAdapter(s3Presigner, bucketName);
    }

    @Bean
    public MovieService movieService(MovieRepositoryOut movieRepositoryOut, MediaStorageOut mediaStorageOut) {
        return new MovieService(movieRepositoryOut, mediaStorageOut);
    }

    @Bean
    public ScreeningRepositoryOut screeningRepositoryOut(SpringDataScreeningRepository springDataScreeningRepository) {
        return new PostgresScreeningAdapter(springDataScreeningRepository);
    }

    @Bean
    public ScreeningService screeningService(ScreeningRepositoryOut screeningRepositoryOut) {
        return new ScreeningService(screeningRepositoryOut);
    }

    @Bean
    public UserRepositoryOut userRepositoryOut(SpringDataUserRepository springDataUserRepository) {
        return new PostgresUserAdapter(springDataUserRepository);
    }

    @Bean
    public PaymentGatewayOut paymentGatewayOut() {
        return new StripePaymentAdapter();
    }

    @Bean
    public QrCodeGeneratorOut qrCodeGeneratorOut() {
        return new ZxingQrCodeAdapter();
    }

    @Bean
    public NotificationOut notificationOut(MediaStorageOut mediaStorageOut) {
        return new EmailNotificationAdapter(mediaStorageOut);
    }

    @Bean
    public BookingRepositoryOut bookingRepositoryOut(SpringDataBookingRepository springDataBookingRepository) {
        return new PostgresBookingAdapter(springDataBookingRepository);
    }

    @Bean
    public BookingService bookingService(
            BookingRepositoryOut bookingRepositoryOut,
            ScreeningRepositoryOut screeningRepositoryOut,
            PaymentGatewayOut paymentGatewayOut,
            NotificationOut notificationOut,
            UserRepositoryOut userRepositoryOut,
            QrCodeGeneratorOut qrCodeGeneratorOut,
            MediaStorageOut mediaStorageOut,
            JwtTokenProvider jwtTokenProvider
    ) {
        return new BookingService(
                bookingRepositoryOut,
                screeningRepositoryOut,
                paymentGatewayOut,
                notificationOut,
                userRepositoryOut,
                qrCodeGeneratorOut,
                mediaStorageOut,
                jwtTokenProvider
        );
    }

    @Bean
    public UserService userService(UserRepositoryOut userRepositoryOut) {
        return new UserService(userRepositoryOut);
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider();
    }

    @Bean
    public AuthService authService(UserRepositoryOut userRepositoryOut, JwtTokenProvider jwtTokenProvider) {
        return new AuthService(userRepositoryOut, jwtTokenProvider);
    }
}
