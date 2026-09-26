package com.cinema.cinema.infrastructure.adapter.in.web.dto;

import com.cinema.cinema.domain.model.BookingFlowContext;
import com.cinema.cinema.domain.model.state.BookingFlowStateName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingFlowResponse(
        String flowSessionId,
        BookingFlowStateName state,
        Long selectedMovieId,
        Long selectedScreeningId,
        Integer selectedSeatCount,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        Long authenticatedUserId,
        Long bookingId,
        LocalDateTime expiresAt,
        String paymentPageUrl,
        String loginUrl,
        String qrCodeUrl
) {
    public static BookingFlowResponse fromDomain(BookingFlowContext context) {
        if (context == null) {
            return null;
        }

        BigDecimal totalAmount = null;
        LocalDateTime expiresAt = null;

        if (context.getBooking() != null) {
            totalAmount = context.getBooking().totalAmount();
            expiresAt = context.getBooking().expiresAt();
        } else if (context.getUnitPrice() != null && context.getSelectedSeatCount() != null) {
            totalAmount = context.getUnitPrice().multiply(BigDecimal.valueOf(context.getSelectedSeatCount()));
        }

        String paymentPageUrl = null;
        String loginUrl = null;

        if (context.getStateName() == BookingFlowStateName.PAYMENT || context.getStateName() == BookingFlowStateName.AUTHENTICATED) {
            paymentPageUrl = "/checkout?session=" + context.getFlowSessionId();
        } else if (context.getStateName() == BookingFlowStateName.LOGIN_REQUIRED) {
            loginUrl = "/login?redirect=/booking/" + context.getFlowSessionId();
        }

        return new BookingFlowResponse(
                context.getFlowSessionId(),
                context.getStateName(),
                context.getSelectedMovieId(),
                context.getSelectedScreeningId(),
                context.getSelectedSeatCount(),
                context.getUnitPrice(),
                totalAmount,
                context.getAuthenticatedUserId(),
                context.getBookingId(),
                expiresAt,
                paymentPageUrl,
                loginUrl,
                context.getQrCodeUrl()
        );
    }
}
