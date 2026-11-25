package com.tracelens.mainbackend.incidents;

import com.tracelens.contracts.incident.IncidentEvent;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentEventProcessor {
    private static final String OPEN = "OPEN";
    private static final String RESOLVED = "RESOLVED";

    private final IncidentRepository incidentRepository;
    private final ProcessedEventRepository processedEventRepository;

    public IncidentEventProcessor(
            IncidentRepository incidentRepository,
            ProcessedEventRepository processedEventRepository) {
        this.incidentRepository = incidentRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void process(IncidentEvent event) {
        validate(event);
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        IncidentEntity incident = switch (event.eventType().trim().toUpperCase(Locale.ROOT)) {
            case "INCIDENT_DETECTED" -> processDetected(event);
            case "INCIDENT_UPDATED" -> processUpdated(event);
            case "INCIDENT_RESOLVED" -> processResolved(event);
            default -> throw new IllegalArgumentException("Unsupported incident event type: " + event.eventType());
        };

        incidentRepository.save(incident);
        processedEventRepository.save(new ProcessedEventEntity(event.eventId()));
    }

    private IncidentEntity processDetected(IncidentEvent event) {
        IncidentEntity incident = incidentRepository.findById(event.incidentId())
                .orElseGet(() -> new IncidentEntity(
                        event.incidentId(),
                        OPEN,
                        event.severity(),
                        event.serviceName(),
                        event.description(),
                        event.occurredAt(),
                        event.occurredAt(),
                        null));
        applyDetails(incident, event);
        incident.setStatus(OPEN);
        incident.setResolvedAt(null);
        incident.setFirstOccurredAt(earlier(incident.getFirstOccurredAt(), event.occurredAt()));
        incident.setLastOccurredAt(later(incident.getLastOccurredAt(), event.occurredAt()));
        return incident;
    }

    private IncidentEntity processUpdated(IncidentEvent event) {
        IncidentEntity incident = incidentRepository.findById(event.incidentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot update missing incident " + event.incidentId()));
        applyDetails(incident, event);
        incident.setLastOccurredAt(later(incident.getLastOccurredAt(), event.occurredAt()));
        return incident;
    }

    private IncidentEntity processResolved(IncidentEvent event) {
        IncidentEntity incident = incidentRepository.findById(event.incidentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot resolve missing incident " + event.incidentId()));
        applyDetails(incident, event);
        incident.setStatus(RESOLVED);
        incident.setLastOccurredAt(later(incident.getLastOccurredAt(), event.occurredAt()));
        incident.setResolvedAt(event.occurredAt());
        return incident;
    }

    private void applyDetails(IncidentEntity incident, IncidentEvent event) {
        incident.setSeverity(event.severity());
        incident.setServiceName(event.serviceName());
        incident.setDescription(event.description());
    }

    private void validate(IncidentEvent event) {
        Objects.requireNonNull(event, "Incident event must not be null");
        Objects.requireNonNull(event.eventId(), "Incident event ID must not be null");
        Objects.requireNonNull(event.incidentId(), "Incident ID must not be null");
        Objects.requireNonNull(event.occurredAt(), "Incident occurrence time must not be null");
        requireText(event.eventType(), "Incident event type");
        requireText(event.serviceName(), "Incident service name");
        requireText(event.severity(), "Incident severity");
        requireText(event.description(), "Incident description");
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private Instant earlier(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private Instant later(Instant first, Instant second) {
        return first.isAfter(second) ? first : second;
    }
}
