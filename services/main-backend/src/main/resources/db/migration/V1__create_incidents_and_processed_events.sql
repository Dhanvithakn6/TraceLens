CREATE TABLE incidents (
    incident_id UUID PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    first_occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY
);
