package com.tracelens.mainbackend.incidents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
public class ProcessedEventEntity {
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    protected ProcessedEventEntity() {
    }

    public ProcessedEventEntity(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getEventId() {
        return eventId;
    }
}
