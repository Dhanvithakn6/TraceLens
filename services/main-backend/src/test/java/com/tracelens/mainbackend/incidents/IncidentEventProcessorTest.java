package com.tracelens.mainbackend.incidents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tracelens.contracts.incident.IncidentEvent;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(IncidentEventProcessor.class)
class IncidentEventProcessorTest {
    private static final UUID INCIDENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final Instant DETECTED_AT = Instant.parse("2026-11-25T10:00:00Z");

    @Autowired
    private IncidentEventProcessor processor;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void createsOpenIncidentFromDetectedEvent() {
        processor.process(event("INCIDENT_DETECTED", "event-detected", DETECTED_AT,
                "HIGH", "payment-service", "Payment errors"));

        IncidentEntity incident = incidentRepository.findById(INCIDENT_ID).orElseThrow();
        assertEquals("OPEN", incident.getStatus());
        assertEquals("HIGH", incident.getSeverity());
        assertEquals("payment-service", incident.getServiceName());
        assertEquals("Payment errors", incident.getDescription());
        assertEquals(DETECTED_AT, incident.getFirstOccurredAt());
        assertEquals(DETECTED_AT, incident.getLastOccurredAt());
        assertEquals(1, processedEventRepository.count());
    }

    @Test
    void ignoresDuplicateEventId() {
        IncidentEvent detected = event("INCIDENT_DETECTED", "event-duplicate", DETECTED_AT,
                "HIGH", "payment-service", "Payment errors");

        processor.process(detected);
        processor.process(detected);

        assertEquals(1, incidentRepository.count());
        assertEquals(1, processedEventRepository.count());
    }

    @Test
    void updatesExistingIncidentDetails() {
        processor.process(event("INCIDENT_DETECTED", "event-detected-update", DETECTED_AT,
                "MEDIUM", "payment-service", "Initial details"));
        Instant updatedAt = DETECTED_AT.plusSeconds(60);
        processor.process(event("INCIDENT_UPDATED", "event-updated", updatedAt,
                "HIGH", "payment-service", "Updated details"));

        IncidentEntity incident = incidentRepository.findById(INCIDENT_ID).orElseThrow();
        assertEquals("OPEN", incident.getStatus());
        assertEquals("HIGH", incident.getSeverity());
        assertEquals("Updated details", incident.getDescription());
        assertEquals(updatedAt, incident.getLastOccurredAt());
        assertEquals(2, processedEventRepository.count());
    }

    @Test
    void resolvesExistingIncident() {
        processor.process(event("INCIDENT_DETECTED", "event-detected-resolve", DETECTED_AT,
                "HIGH", "payment-service", "Payment errors"));
        Instant resolvedAt = DETECTED_AT.plusSeconds(300);
        processor.process(event("INCIDENT_RESOLVED", "event-resolved", resolvedAt,
                "HIGH", "payment-service", "Payment errors recovered"));

        IncidentEntity incident = incidentRepository.findById(INCIDENT_ID).orElseThrow();
        assertEquals("RESOLVED", incident.getStatus());
        assertEquals(resolvedAt, incident.getResolvedAt());
        assertEquals(resolvedAt, incident.getLastOccurredAt());
        assertEquals("Payment errors recovered", incident.getDescription());
        assertEquals(2, processedEventRepository.count());
        assertTrue(processedEventRepository.existsById(eventId("event-resolved")));
    }

    private IncidentEvent event(
            String eventType,
            String eventIdSeed,
            Instant occurredAt,
            String severity,
            String serviceName,
            String description) {
        return new IncidentEvent(
                eventId(eventIdSeed),
                INCIDENT_ID,
                eventType,
                serviceName,
                severity,
                description,
                occurredAt);
    }

    private UUID eventId(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
