package com.cinema.cinema.domain.port.out;

import java.math.BigDecimal;

public interface PaymentGatewayOut {
    boolean processPayment(Long bookingId, BigDecimal amount, String paymentToken);
}
