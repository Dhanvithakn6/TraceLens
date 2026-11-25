package com.tracelens.mainbackend.kafka;

import com.tracelens.contracts.incident.IncidentEvent;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class IncidentEventProducer {
    private final KafkaTemplate<String, IncidentEvent> kafkaTemplate;
    private final String incidentsTopic;

    public IncidentEventProducer(
            KafkaTemplate<String, IncidentEvent> kafkaTemplate,
            @Value("${tracelens.kafka.incidents-topic}") String incidentsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.incidentsTopic = incidentsTopic;
    }

    public CompletableFuture<SendResult<String, IncidentEvent>> publish(IncidentEvent event) {
        return kafkaTemplate.send(incidentsTopic, event.incidentId().toString(), event);
    }
}
