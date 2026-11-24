package com.tracelens.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
    @Test
    void processesAndRetrievesSuccessfulPayment() {
        PaymentService service = new PaymentService(0, false);
        Payment created = service.process(new PaymentRequest("order-123", new BigDecimal("19.99")));

        assertNotNull(created.paymentId());
        assertEquals("order-123", created.orderId());
        assertEquals(new BigDecimal("19.99"), created.amount());
        assertEquals(PaymentStatus.SUCCEEDED, created.status());
        assertEquals(created, service.get(created.paymentId()));
    }

    @Test
    void recordsSimulatedFailure() {
        PaymentService service = new PaymentService(0, true);
        Payment payment = service.process(new PaymentRequest("order-456", new BigDecimal("5.00")));

        assertEquals(PaymentStatus.FAILED, payment.status());
    }

    @Test
    void throwsWhenPaymentDoesNotExist() {
        PaymentService service = new PaymentService(0, false);

        assertThrows(PaymentNotFoundException.class, () -> service.get(UUID.randomUUID()));
    }
}
