package com.medsafe.service;

import com.medsafe.dto.AIExplanationRequest;
import com.medsafe.dto.AIExplanationResponse;

public interface AIExplanationService {
    AIExplanationResponse explainInteraction(AIExplanationRequest request);
}
