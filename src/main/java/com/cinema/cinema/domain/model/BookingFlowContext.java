package com.cinema.cinema.domain.model;

import com.cinema.cinema.domain.model.state.BookingFlowState;
import com.cinema.cinema.domain.model.state.BookingFlowStateName;
import com.cinema.cinema.domain.model.state.SelectMovieState;

import java.math.BigDecimal;
import java.util.UUID;

public class BookingFlowContext {

    private String flowSessionId;
    private BookingFlowState currentState;
    private Long selectedMovieId;
    private Long selectedScreeningId;
    private Integer selectedSeatCount;
    private BigDecimal unitPrice;
    private Long authenticatedUserId;
    private Long bookingId;
    private String qrCodeUrl;
    private byte[] qrCodeData;
    private Booking booking;

    public BookingFlowContext() {
        this(UUID.randomUUID().toString());
    }

    public BookingFlowContext(String flowSessionId) {
        this.flowSessionId = flowSessionId != null ? flowSessionId : UUID.randomUUID().toString();
        this.currentState = new SelectMovieState();
    }

    public void setState(BookingFlowState state) {
        this.currentState = state;
    }

    public BookingFlowState getCurrentState() {
        return currentState;
    }

    public BookingFlowStateName getStateName() {
        return currentState != null ? currentState.getName() : null;
    }

    public String getFlowSessionId() {
        return flowSessionId;
    }

    public void setFlowSessionId(String flowSessionId) {
        this.flowSessionId = flowSessionId;
    }

    public Long getSelectedMovieId() {
        return selectedMovieId;
    }

    public void setSelectedMovieId(Long selectedMovieId) {
        this.selectedMovieId = selectedMovieId;
    }

    public Long getSelectedScreeningId() {
        return selectedScreeningId;
    }

    public void setSelectedScreeningId(Long selectedScreeningId) {
        this.selectedScreeningId = selectedScreeningId;
    }

    public Integer getSelectedSeatCount() {
        return selectedSeatCount;
    }

    public void setSelectedSeatCount(Integer selectedSeatCount) {
        this.selectedSeatCount = selectedSeatCount;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getAuthenticatedUserId() {
        return authenticatedUserId;
    }

    public void setAuthenticatedUserId(Long authenticatedUserId) {
        this.authenticatedUserId = authenticatedUserId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public byte[] getQrCodeData() {
        return qrCodeData;
    }

    public void setQrCodeData(byte[] qrCodeData) {
        this.qrCodeData = qrCodeData;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
        if (booking != null && booking.id() != null) {
            this.bookingId = booking.id();
        }
    }

    public void selectMovie(Long movieId) {
        if (this.currentState == null) {
            throw new IllegalStateException("Current state is not initialized");
        }
        this.currentState.selectMovie(this, movieId);
    }

    public void selectScreening(Long screeningId) {
        if (this.currentState == null) {
            throw new IllegalStateException("Current state is not initialized");
        }
        this.currentState.selectScreening(this, screeningId);
    }

    public void selectSeats(int seatCount) {
        if (this.currentState == null) {
            throw new IllegalStateException("Current state is not initialized");
        }
        this.currentState.selectSeats(this, seatCount);
    }

    public void authenticate(Long userId) {
        if (this.currentState == null) {
            throw new IllegalStateException("Current state is not initialized");
        }
        this.currentState.authenticate(this, userId);
    }

    public void processPayment(String paymentToken) {
        if (this.currentState == null) {
            throw new IllegalStateException("Current state is not initialized");
        }
        this.currentState.processPayment(this, paymentToken);
    }

    public void cancel() {
        if (this.currentState == null) {
            throw new IllegalStateException("Current state is not initialized");
        }
        this.currentState.cancel(this);
    }
}
