package com.medsafe.controller;

import com.medsafe.dto.InteractionAlertResponse;
import com.medsafe.dto.InteractionCheckRequest;
import com.medsafe.dto.InteractionCheckResponse;
import com.medsafe.service.InteractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/check")
    public ResponseEntity<InteractionCheckResponse> checkInteractions(@RequestBody(required = false) InteractionCheckRequest request) {
        InteractionCheckResponse response = interactionService.checkInteractions(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<InteractionAlertResponse>> getAlerts() {
        List<InteractionAlertResponse> alerts = interactionService.getAlertsForCurrentUser();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InteractionAlertResponse> getAlertById(@PathVariable("id") Long id) {
        InteractionAlertResponse alert = interactionService.getAlertById(id);
        return ResponseEntity.ok(alert);
    }
}
