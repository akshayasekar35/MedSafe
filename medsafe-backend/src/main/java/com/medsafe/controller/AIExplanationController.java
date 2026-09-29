package com.medsafe.controller;

import com.medsafe.dto.AIExplanationRequest;
import com.medsafe.dto.AIExplanationResponse;
import com.medsafe.service.AIExplanationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AIExplanationController {

    private final AIExplanationService aiExplanationService;

    public AIExplanationController(AIExplanationService aiExplanationService) {
        this.aiExplanationService = aiExplanationService;
    }

    @PostMapping("/explain-interaction")
    public ResponseEntity<AIExplanationResponse> explainInteraction(@Valid @RequestBody AIExplanationRequest request) {
        AIExplanationResponse response = aiExplanationService.explainInteraction(request);
        return ResponseEntity.ok(response);
    }
}
