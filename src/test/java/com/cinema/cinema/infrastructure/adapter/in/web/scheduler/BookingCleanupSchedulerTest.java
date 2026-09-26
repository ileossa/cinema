package com.cinema.cinema.infrastructure.adapter.in.web.scheduler;

import com.cinema.cinema.domain.port.in.ReleaseExpiredBookingsUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BookingCleanupSchedulerTest {

    @Test
    void cleanupExpiredBookings_shouldInvokeReleaseExpiredBookingsUseCase() {
        ReleaseExpiredBookingsUseCase useCase = mock(ReleaseExpiredBookingsUseCase.class);
        BookingCleanupScheduler scheduler = new BookingCleanupScheduler(useCase);

        scheduler.cleanupExpiredBookings();

        verify(useCase).releaseExpiredBookings();
    }
}
