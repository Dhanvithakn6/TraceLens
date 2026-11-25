package com.tracelens.contracts.incident;

import java.time.Instant;
import java.util.UUID;

/** Shared Kafka contract for incident lifecycle events. */
public record IncidentEvent(
        UUID eventId,
        UUID incidentId,
        String eventType,
        String serviceName,
        String severity,
        String description,
        Instant occurredAt) {
}
