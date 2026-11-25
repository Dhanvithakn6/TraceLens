package com.tracelens.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
    @Test
    void processesAndRetrievesSuccessfulPayment() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        when(tracer.currentSpan()).thenReturn(span);
        PaymentService service = new PaymentService(0, false, tracer);
        Payment created = service.process(new PaymentRequest("order-123", new BigDecimal("19.99")));

        assertNotNull(created.paymentId());
        assertEquals("order-123", created.orderId());
        assertEquals(new BigDecimal("19.99"), created.amount());
        assertEquals(PaymentStatus.SUCCEEDED, created.status());
        assertEquals(created, service.get(created.paymentId()));
        verify(span).tag("payment.status", "SUCCEEDED");
        verify(span, never()).event("payment.failed");
    }

    @Test
    void recordsSimulatedFailure() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        when(tracer.currentSpan()).thenReturn(span);
        PaymentService service = new PaymentService(0, true, tracer);
        Payment payment = service.process(new PaymentRequest("order-456", new BigDecimal("5.00")));

        assertEquals(PaymentStatus.FAILED, payment.status());
        assertEquals(payment, service.get(payment.paymentId()));
        verify(span).tag("payment.status", "FAILED");
        verify(span).event("payment.failed");
    }

    @Test
    void processesPaymentWhenThereIsNoCurrentSpan() {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);
        PaymentService service = new PaymentService(0, true, tracer);

        Payment payment = service.process(new PaymentRequest("order-789", new BigDecimal("3.00")));

        assertEquals(PaymentStatus.FAILED, payment.status());
        assertEquals(payment, service.get(payment.paymentId()));
    }

    @Test
    void throwsWhenPaymentDoesNotExist() {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);
        PaymentService service = new PaymentService(0, false, tracer);

        assertThrows(PaymentNotFoundException.class, () -> service.get(UUID.randomUUID()));
    }
}
