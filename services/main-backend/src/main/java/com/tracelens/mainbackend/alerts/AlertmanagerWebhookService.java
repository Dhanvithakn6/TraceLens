package com.tracelens.mainbackend.alerts;

import com.tracelens.mainbackend.kafka.IncidentEventProducer;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlertmanagerWebhookService {
    private final AlertmanagerIncidentMapper mapper;
    private final IncidentEventProducer eventProducer;

    public AlertmanagerWebhookService(AlertmanagerIncidentMapper mapper, IncidentEventProducer eventProducer) {
        this.mapper = mapper;
        this.eventProducer = eventProducer;
    }

    public AlertmanagerWebhookResponse handle(AlertmanagerWebhookRequest request) {
        if (request == null || request.alerts() == null || request.alerts().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one alert is required");
        }

        List<AlertmanagerIncidentMapper.MappedAlert> mapped = request.alerts().stream()
                .map(alert -> mapper.map(alert, request.status()))
                .collect(Collectors.toList());
        List<AlertmanagerIncidentMapper.MappedAlert> firing = mapped.stream()
                .filter(alert -> alert.status().equals("firing"))
                .toList();
        int resolvedCount = mapped.size() - firing.size();

        try {
            for (AlertmanagerIncidentMapper.MappedAlert alert : firing) {
                eventProducer.publish(alert.event()).join();
            }
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Unable to publish incident alert", exception);
        }

        return new AlertmanagerWebhookResponse(mapped.size(), firing.size(), resolvedCount);
    }
}
