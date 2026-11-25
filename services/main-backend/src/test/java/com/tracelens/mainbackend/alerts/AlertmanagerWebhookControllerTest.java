package com.tracelens.mainbackend.alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tracelens.contracts.incident.IncidentEvent;
import com.tracelens.mainbackend.kafka.IncidentEventProducer;
import java.time.Instant;
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
    void acknowledgesResolvedAlertWithoutPublishingIncident() throws Exception {
        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"resolved",
                                  "alerts":[{
                                    "status":"resolved",
                                    "labels":{"alertname":"TraceLensServiceUnavailable","job":"user-service"},
                                    "annotations":{"summary":"User Service recovered"},
                                    "endsAt":"2026-11-25T10:05:00Z"
                                  }]
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.receivedAlerts").value(1))
                .andExpect(jsonPath("$.firingAlertsPublished").value(0))
                .andExpect(jsonPath("$.resolvedAlertsAcknowledged").value(1));

        verify(eventProducer, never()).publish(any(IncidentEvent.class));
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
}
