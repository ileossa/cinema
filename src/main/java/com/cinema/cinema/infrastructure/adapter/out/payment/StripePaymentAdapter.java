package com.cinema.cinema.infrastructure.adapter.out.payment;

import com.cinema.cinema.domain.port.out.PaymentGatewayOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class StripePaymentAdapter implements PaymentGatewayOut {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentAdapter.class);

    @Override
    public boolean processPayment(Long bookingId, BigDecimal amount, String paymentToken) {
        log.info("Processing Stripe payment simulation for bookingId={}, amount={}, paymentToken={}", bookingId, amount, paymentToken);
        if ("fail".equalsIgnoreCase(paymentToken) || "tok_chargeDeclined".equalsIgnoreCase(paymentToken)) {
            log.warn("Stripe payment simulation failed for bookingId={}", bookingId);
            return false;
        }
        log.info("Stripe payment successful for bookingId={}", bookingId);
        return true;
    }
}
