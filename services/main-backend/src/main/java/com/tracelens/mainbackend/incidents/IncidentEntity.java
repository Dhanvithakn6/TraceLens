package com.tracelens.mainbackend.incidents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class IncidentEntity {
    @Id
    @Column(name = "incident_id", nullable = false, updatable = false)
    private UUID incidentId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 32)
    private String severity;

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(name = "first_occurred_at", nullable = false)
    private Instant firstOccurredAt;

    @Column(name = "last_occurred_at", nullable = false)
    private Instant lastOccurredAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected IncidentEntity() {
    }

    public IncidentEntity(
            UUID incidentId,
            String status,
            String severity,
            String serviceName,
            String description,
            Instant firstOccurredAt,
            Instant lastOccurredAt,
            Instant resolvedAt) {
        this.incidentId = incidentId;
        this.status = status;
        this.severity = severity;
        this.serviceName = serviceName;
        this.description = description;
        this.firstOccurredAt = firstOccurredAt;
        this.lastOccurredAt = lastOccurredAt;
        this.resolvedAt = resolvedAt;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getFirstOccurredAt() {
        return firstOccurredAt;
    }

    public void setFirstOccurredAt(Instant firstOccurredAt) {
        this.firstOccurredAt = firstOccurredAt;
    }

    public Instant getLastOccurredAt() {
        return lastOccurredAt;
    }

    public void setLastOccurredAt(Instant lastOccurredAt) {
        this.lastOccurredAt = lastOccurredAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
