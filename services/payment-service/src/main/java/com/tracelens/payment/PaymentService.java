package com.tracelens.payment;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final ConcurrentMap<UUID, Payment> payments = new ConcurrentHashMap<>();
    private final long delayMs;
    private final boolean simulateFailure;
    private final Tracer tracer;

    public PaymentService(
            @Value("${payment.simulation.delay-ms:0}") long delayMs,
            @Value("${payment.simulation.failure:false}") boolean simulateFailure,
            Tracer tracer) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("payment.simulation.delay-ms must be non-negative");
        }
        this.delayMs = delayMs;
        this.simulateFailure = simulateFailure;
        this.tracer = tracer;
    }

    public Payment process(PaymentRequest request) {
        delayIfConfigured();
        Payment payment = new Payment(UUID.randomUUID(), request.orderId(), request.amount(),
                simulateFailure ? PaymentStatus.FAILED : PaymentStatus.SUCCEEDED);
        recordOutcome(payment.status());
        payments.put(payment.paymentId(), payment);
        return payment;
    }

    private void recordOutcome(PaymentStatus status) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return;
        }

        currentSpan.tag("payment.status", status.name());
        if (status == PaymentStatus.FAILED) {
            currentSpan.event("payment.failed");
        }
    }

    public Payment get(UUID id) {
        Payment payment = payments.get(id);
        if (payment == null) {
            throw new PaymentNotFoundException(id);
        }
        return payment;
    }

    private void delayIfConfigured() {
        if (delayMs == 0) return;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Payment processing was interrupted", exception);
        }
    }
}
