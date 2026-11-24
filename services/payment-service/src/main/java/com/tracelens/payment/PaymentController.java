package com.tracelens.payment;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping
    public ResponseEntity<Payment> process(@Valid @RequestBody PaymentRequest request) {
        Payment payment = payments.process(request);
        return ResponseEntity.created(URI.create("/api/payments/" + payment.paymentId())).body(payment);
    }

    @GetMapping("/{id}")
    public Payment get(@PathVariable UUID id) {
        return payments.get(id);
    }
}
