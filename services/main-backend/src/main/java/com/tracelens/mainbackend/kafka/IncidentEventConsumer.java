package com.tracelens.mainbackend.kafka;

import com.tracelens.contracts.incident.IncidentEvent;
import com.tracelens.mainbackend.incidents.IncidentEventProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IncidentEventConsumer {
    private final IncidentEventProcessor eventProcessor;

    public IncidentEventConsumer(IncidentEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @KafkaListener(
            topics = "${tracelens.kafka.incidents-topic}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consume(IncidentEvent event) {
        eventProcessor.process(event);
    }
}
