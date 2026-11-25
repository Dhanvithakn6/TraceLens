package com.tracelens.mainbackend.alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tracelens.contracts.incident.IncidentEvent;
import com.tracelens.mainbackend.kafka.IncidentEventProducer;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlertmanagerWebhookController.class)
@Import({AlertmanagerWebhookService.class, AlertmanagerIncidentMapper.class})
class AlertmanagerWebhookControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentEventProducer eventProducer;

    @Test
    void publishesFiringAlertAsIncidentEvent() throws Exception {
        when(eventProducer.publish(any(IncidentEvent.class))).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firingPayload()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.receivedAlerts").value(1))
                .andExpect(jsonPath("$.firingAlertsPublished").value(1))
                .andExpect(jsonPath("$.resolvedAlertsAcknowledged").value(0));

        var eventCaptor = org.mockito.ArgumentCaptor.forClass(IncidentEvent.class);
        verify(eventProducer).publish(eventCaptor.capture());
        IncidentEvent event = eventCaptor.getValue();
        assertEquals("INCIDENT_DETECTED", event.eventType());
        assertEquals("payment-service", event.serviceName());
        assertEquals("HIGH", event.severity());
        assertEquals("Elevated 5xx rate", event.description());
        assertEquals(Instant.parse("2026-11-25T10:00:00Z"), event.occurredAt());
    }

    @Test
    void publishesResolutionEventCorrelatedWithFiringEvent() throws Exception {
        when(eventProducer.publish(any(IncidentEvent.class))).thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firingPayload()))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolvedPayload()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.receivedAlerts").value(1))
                .andExpect(jsonPath("$.firingAlertsPublished").value(0))
                .andExpect(jsonPath("$.resolvedAlertsAcknowledged").value(1));

        var eventCaptor = org.mockito.ArgumentCaptor.forClass(IncidentEvent.class);
        verify(eventProducer, times(2)).publish(eventCaptor.capture());
        List<IncidentEvent> events = eventCaptor.getAllValues();
        IncidentEvent firing = events.get(0);
        IncidentEvent resolved = events.get(1);
        assertEquals("INCIDENT_RESOLVED", resolved.eventType());
        assertEquals(firing.incidentId(), resolved.incidentId());
        assertNotEquals(firing.eventId(), resolved.eventId());
        assertEquals("payment-service", resolved.serviceName());
        assertEquals("HIGH", resolved.severity());
        assertEquals("Payment recovered", resolved.description());
        assertEquals(Instant.parse("2026-11-25T10:05:00Z"), resolved.occurredAt());
    }

    @Test
    void repeatedResolvedNotificationsUseTheSameEventId() throws Exception {
        when(eventProducer.publish(any(IncidentEvent.class))).thenReturn(CompletableFuture.completedFuture(null));
        String resolvedPayload = resolvedPayload();

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolvedPayload))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolvedPayload))
                .andExpect(status().isAccepted());

        var eventCaptor = org.mockito.ArgumentCaptor.forClass(IncidentEvent.class);
        verify(eventProducer, times(2)).publish(eventCaptor.capture());
        List<IncidentEvent> events = eventCaptor.getAllValues();
        assertEquals(events.get(0).eventId(), events.get(1).eventId());
        assertEquals(events.get(0).incidentId(), events.get(1).incidentId());
        assertEquals("INCIDENT_RESOLVED", events.get(0).eventType());
    }

    @Test
    void rejectsInvalidAlertPayload() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"firing",
                                  "alerts":[{
                                    "status":"firing",
                                    "labels":{"job":"payment-service"}
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(eventProducer, never()).publish(any(IncidentEvent.class));
    }

    private String firingPayload() {
        return """
                {
                  "status":"firing",
                  "alerts":[{
                    "status":"firing",
                    "labels":{
                      "alertname":"TraceLensHighHttp5xxRate",
                      "job":"payment-service",
                      "severity":"warning"
                    },
                    "annotations":{"summary":"Elevated 5xx rate"},
                    "startsAt":"2026-11-25T10:00:00Z",
                    "endsAt":"0001-01-01T00:00:00Z",
                    "fingerprint":"a1b2c3d4"
                  }]
                }
                """;
    }

    private String resolvedPayload() {
        return """
                {
                  "status":"resolved",
                  "alerts":[{
                    "status":"resolved",
                    "labels":{
                      "alertname":"TraceLensHighHttp5xxRate",
                      "job":"payment-service",
                      "severity":"warning"
                    },
                    "annotations":{"summary":"Payment recovered"},
                    "startsAt":"2026-11-25T10:00:00Z",
                    "endsAt":"2026-11-25T10:05:00Z",
                    "fingerprint":"a1b2c3d4"
                  }]
                }
                """;
    }
}
