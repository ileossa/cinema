package com.cinema.cinema.infrastructure.adapter.in.web.scheduler;

import com.cinema.cinema.domain.port.in.ReleaseExpiredBookingsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingCleanupScheduler {

    private final ReleaseExpiredBookingsUseCase releaseExpiredBookingsUseCase;

    public BookingCleanupScheduler(ReleaseExpiredBookingsUseCase releaseExpiredBookingsUseCase) {
        this.releaseExpiredBookingsUseCase = releaseExpiredBookingsUseCase;
    }

    // Exécution toutes les minutes (60 000 ms)
    @Scheduled(fixedDelay = 60000)
    public void cleanupExpiredBookings() {
        releaseExpiredBookingsUseCase.releaseExpiredBookings();
    }
}
