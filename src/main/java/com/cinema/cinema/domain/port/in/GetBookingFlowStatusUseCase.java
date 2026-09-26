package com.cinema.cinema.domain.port.in;

import com.cinema.cinema.domain.model.BookingFlowContext;

public interface GetBookingFlowStatusUseCase {
    BookingFlowContext getFlowStatus(String flowSessionId);
}
