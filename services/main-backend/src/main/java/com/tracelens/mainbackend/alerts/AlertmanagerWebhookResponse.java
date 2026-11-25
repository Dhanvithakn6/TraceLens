package com.tracelens.mainbackend.alerts;

public record AlertmanagerWebhookResponse(
        int receivedAlerts,
        int firingAlertsPublished,
        int resolvedAlertsAcknowledged) {
}
