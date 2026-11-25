package com.tracelens.mainbackend.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutResponse(UserDetails user, OrderDetails order, PaymentDetails payment) {
    public record UserDetails(UUID userId, String name, String email, String status) {
    }

    public record OrderDetails(
            UUID orderId,
            String userId,
            List<CheckoutRequest.CheckoutItem> items,
            BigDecimal totalAmount,
            String status) {
    }

    public record PaymentDetails(UUID paymentId, String orderId, BigDecimal amount, String status) {
    }
}
