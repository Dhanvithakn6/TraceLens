package com.tracelens.mainbackend.alerts;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AlertmanagerWebhookRequest(String status, List<Alert> alerts) {
    public record Alert(
            String status,
            Map<String, String> labels,
            Map<String, String> annotations,
            Instant startsAt,
            Instant endsAt,
            String fingerprint) {
    }
}
