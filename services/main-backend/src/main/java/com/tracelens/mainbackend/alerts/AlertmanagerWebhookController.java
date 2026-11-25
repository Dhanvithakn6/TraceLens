package com.tracelens.mainbackend.alerts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertmanagerWebhookController {
    private final AlertmanagerWebhookService webhookService;

    public AlertmanagerWebhookController(AlertmanagerWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    public ResponseEntity<AlertmanagerWebhookResponse> receive(@RequestBody AlertmanagerWebhookRequest request) {
        return ResponseEntity.accepted().body(webhookService.handle(request));
    }
}
