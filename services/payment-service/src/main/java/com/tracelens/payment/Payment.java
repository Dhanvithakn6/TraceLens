package com.tracelens.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record Payment(UUID paymentId, String orderId, BigDecimal amount, PaymentStatus status) {
}
