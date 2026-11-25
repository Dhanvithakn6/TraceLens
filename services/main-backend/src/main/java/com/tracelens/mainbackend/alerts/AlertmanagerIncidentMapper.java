package com.tracelens.mainbackend.alerts;

import com.tracelens.contracts.incident.IncidentEvent;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AlertmanagerIncidentMapper {
    public MappedAlert map(AlertmanagerWebhookRequest.Alert alert, String webhookStatus) {
        if (alert == null) {
            throw invalid("Alert entry must not be null");
        }

        String status = nonBlank(alert.status()) ? alert.status() : webhookStatus;
        if (!nonBlank(status)) {
            throw invalid("Alert status is required");
        }
        status = status.trim().toLowerCase(Locale.ROOT);
        if (!status.equals("firing") && !status.equals("resolved")) {
            throw invalid("Unsupported alert status: " + status);
        }

        Map<String, String> labels = alert.labels() == null ? Map.of() : alert.labels();
        Map<String, String> annotations = alert.annotations() == null ? Map.of() : alert.annotations();
        String alertName = firstNonBlank(labels.get("alertname"));
        if (alertName == null) {
            throw invalid("Alert label 'alertname' is required");
        }

        if (status.equals("resolved")) {
            return new MappedAlert(status, null);
        }

        Instant occurredAt = alert.startsAt() == null ? Instant.now() : alert.startsAt();
        String serviceName = firstNonBlank(labels.get("service"), labels.get("service_name"), labels.get("job"));
        if (serviceName == null) {
            serviceName = "unknown";
        }
        String description = firstNonBlank(annotations.get("description"), annotations.get("summary"), alertName);
        String severity = mapSeverity(labels.get("severity"));

        String identity = firstNonBlank(alert.fingerprint(), stableLabels(labels));
        String incidentIdentity = "tracelens-alert:" + identity + ":"
                + (alert.startsAt() == null ? "unknown-start" : alert.startsAt());
        UUID incidentId = stableUuid(incidentIdentity);
        UUID eventId = stableUuid(incidentId + ":INCIDENT_DETECTED");

        return new MappedAlert(status, new IncidentEvent(
                eventId,
                incidentId,
                "INCIDENT_DETECTED",
                serviceName,
                severity,
                description,
                occurredAt));
    }

    private String mapSeverity(String severity) {
        if (!nonBlank(severity)) {
            return "UNKNOWN";
        }
        return switch (severity.trim().toLowerCase(Locale.ROOT)) {
            case "critical", "fatal" -> "CRITICAL";
            case "warning", "warn", "error" -> "HIGH";
            case "info", "informational", "notice" -> "LOW";
            case "low" -> "LOW";
            case "medium" -> "MEDIUM";
            case "high" -> "HIGH";
            case "unknown" -> "UNKNOWN";
            default -> severity.trim().toUpperCase(Locale.ROOT);
        };
    }

    private String stableLabels(Map<String, String> labels) {
        return labels.get("alertname") + ":" + new TreeMap<>(labels);
    }

    private UUID stableUuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (nonBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record MappedAlert(String status, IncidentEvent event) {
    }
}
