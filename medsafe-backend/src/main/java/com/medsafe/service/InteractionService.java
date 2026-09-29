package com.medsafe.service;

import com.medsafe.dto.InteractionAlertResponse;
import com.medsafe.dto.InteractionCheckRequest;
import com.medsafe.dto.InteractionCheckResponse;
import com.medsafe.entity.InteractionAlert;
import com.medsafe.entity.Medication;
import com.medsafe.exception.BadRequestException;
import com.medsafe.exception.ResourceNotFoundException;
import com.medsafe.exception.UnauthorizedException;
import com.medsafe.repository.InteractionAlertRepository;
import com.medsafe.repository.MedicationRepository;
import com.medsafe.security.SecurityUtils;
import com.medsafe.service.drugdata.DrugDataProvider;
import com.medsafe.service.drugdata.DrugInteractionResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class InteractionService {

    public static final String SAFETY_DISCLAIMER =
            "The absence of a documented interaction in the consulted reference source does NOT guarantee the complete absence of all potential risks. Always consult a qualified healthcare professional or pharmacist before altering any prescribed medication.";

    private final InteractionAlertRepository interactionAlertRepository;
    private final MedicationRepository medicationRepository;
    private final DrugDataProvider drugDataProvider;
    private final SecurityUtils securityUtils;

    public InteractionService(InteractionAlertRepository interactionAlertRepository,
                              MedicationRepository medicationRepository,
                              DrugDataProvider drugDataProvider,
                              SecurityUtils securityUtils) {
        this.interactionAlertRepository = interactionAlertRepository;
        this.medicationRepository = medicationRepository;
        this.drugDataProvider = drugDataProvider;
        this.securityUtils = securityUtils;
    }

    @Transactional
    public InteractionCheckResponse checkInteractions(InteractionCheckRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();

        List<Medication> targetMeds;
        if (request != null && request.getMedicationIds() != null && !request.getMedicationIds().isEmpty()) {
            Set<Long> uniqueIds = new LinkedHashSet<>(request.getMedicationIds());
            if (uniqueIds.size() < 2) {
                throw new BadRequestException("At least two distinct medications are required to perform an interaction check.");
            }
            targetMeds = medicationRepository.findAllById(uniqueIds).stream()
                    .filter(m -> m.getUser().getUserId().equals(currentUserId))
                    .collect(Collectors.toList());

            if (targetMeds.size() != uniqueIds.size()) {
                throw new UnauthorizedException("One or more requested medications do not belong to your account.");
            }
        } else {
            targetMeds = medicationRepository.findByUserUserIdAndStatus(currentUserId, "ACTIVE");
        }

        List<InteractionAlertResponse> alertResponses = new ArrayList<>();

        // Perform pairwise check
        for (int i = 0; i < targetMeds.size(); i++) {
            for (int j = i + 1; j < targetMeds.size(); j++) {
                Medication medA = targetMeds.get(i);
                Medication medB = targetMeds.get(j);

                List<String> namesA = new ArrayList<>();
                if (medA.getActiveIngredient() != null && !medA.getActiveIngredient().isBlank()) {
                    namesA.add(medA.getActiveIngredient());
                }
                if (medA.getMedicineName() != null && !medA.getMedicineName().isBlank()) {
                    namesA.add(medA.getMedicineName());
                }

                List<String> namesB = new ArrayList<>();
                if (medB.getActiveIngredient() != null && !medB.getActiveIngredient().isBlank()) {
                    namesB.add(medB.getActiveIngredient());
                }
                if (medB.getMedicineName() != null && !medB.getMedicineName().isBlank()) {
                    namesB.add(medB.getMedicineName());
                }

                Optional<DrugInteractionResult> interactionOpt = Optional.empty();
                searchLoop:
                for (String a : namesA) {
                    for (String b : namesB) {
                        interactionOpt = drugDataProvider.checkInteraction(a, b);
                        if (interactionOpt.isPresent() && interactionOpt.get().isHasInteraction()) {
                            break searchLoop;
                        }
                    }
                }

                if (interactionOpt.isPresent()) {
                    DrugInteractionResult result = interactionOpt.get();

                    if (result.isHasInteraction()) {
                        // Persist to interaction_alerts table
                        InteractionAlert alert = new InteractionAlert();
                        alert.setMedication1(medA);
                        alert.setMedication2(medB);
                        alert.setSeverity(result.getSeverity());
                        alert.setDescription(result.getDescription());
                        alert.setSourceName(result.getSourceName());
                        alert.setSourceReference(result.getSourceReference());

                        InteractionAlert savedAlert = interactionAlertRepository.save(alert);
                        InteractionAlertResponse resp = mapToResponse(savedAlert);
                        resp.setEvidenceStatus(result.getEvidenceStatus() != null ? result.getEvidenceStatus().name() : "VERIFIED");
                        resp.setActiveIngredient1(medA.getActiveIngredient());
                        resp.setActiveIngredient2(medB.getActiveIngredient());
                        resp.setRxcui1(result.getRxcuiA());
                        resp.setRxcui2(result.getRxcuiB());
                        alertResponses.add(resp);
                    } else if (result.getEvidenceStatus() == com.medsafe.service.drugdata.model.EvidenceStatus.AMBIGUOUS) {
                        InteractionAlertResponse ambiguousResp = new InteractionAlertResponse(
                                null,
                                medA.getMedicationId(),
                                medA.getMedicineName(),
                                medB.getMedicationId(),
                                medB.getMedicineName(),
                                "AMBIGUOUS",
                                "Please confirm the medicine before checking interactions.",
                                result.getSourceName(),
                                result.getSourceReference(),
                                java.time.LocalDateTime.now(),
                                SAFETY_DISCLAIMER
                        );
                        ambiguousResp.setEvidenceStatus("AMBIGUOUS");
                        ambiguousResp.setActiveIngredient1(medA.getActiveIngredient());
                        ambiguousResp.setActiveIngredient2(medB.getActiveIngredient());
                        ambiguousResp.setAmbiguousOptions(result.getAmbiguousOptions());
                        alertResponses.add(ambiguousResp);
                    } else if (result.getEvidenceStatus() == com.medsafe.service.drugdata.model.EvidenceStatus.SOURCE_UNAVAILABLE) {
                        InteractionAlertResponse unavailResp = new InteractionAlertResponse(
                                null,
                                medA.getMedicationId(),
                                medA.getMedicineName(),
                                medB.getMedicationId(),
                                medB.getMedicineName(),
                                "UNAVAILABLE",
                                "External clinical source is temporarily unavailable. Verification could not be completed.",
                                result.getSourceName(),
                                result.getSourceReference(),
                                java.time.LocalDateTime.now(),
                                SAFETY_DISCLAIMER
                        );
                        unavailResp.setEvidenceStatus("SOURCE_UNAVAILABLE");
                        unavailResp.setActiveIngredient1(medA.getActiveIngredient());
                        unavailResp.setActiveIngredient2(medB.getActiveIngredient());
                        alertResponses.add(unavailResp);
                    }
                }
            }
        }

        String summary;
        if (alertResponses.isEmpty()) {
            summary = "No interaction identified in the consulted source (" + drugDataProvider.getProviderName() + ").";
        } else {
            summary = "Identified " + alertResponses.size() + " documented interaction(s) requiring attention.";
        }

        return new InteractionCheckResponse(
                alertResponses.size(),
                alertResponses,
                summary,
                SAFETY_DISCLAIMER
        );
    }

    @Transactional(readOnly = true)
    public List<InteractionAlertResponse> getAlertsForCurrentUser() {
        Long currentUserId = securityUtils.getCurrentUserId();
        return interactionAlertRepository.findByUserId(currentUserId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InteractionAlertResponse getAlertById(Long alertId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        InteractionAlert alert = interactionAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Interaction alert with ID " + alertId + " not found."));

        if (!alert.getMedication1().getUser().getUserId().equals(currentUserId)
                && !alert.getMedication2().getUser().getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not authorized to view this interaction alert.");
        }

        return mapToResponse(alert);
    }

    public InteractionAlertResponse mapToResponse(InteractionAlert alert) {
        InteractionAlertResponse response = new InteractionAlertResponse(
                alert.getAlertId(),
                alert.getMedication1().getMedicationId(),
                alert.getMedication1().getMedicineName(),
                alert.getMedication2().getMedicationId(),
                alert.getMedication2().getMedicineName(),
                alert.getSeverity(),
                alert.getDescription(),
                alert.getSourceName(),
                alert.getSourceReference(),
                alert.getCheckedAt(),
                SAFETY_DISCLAIMER
        );
        response.setActiveIngredient1(alert.getMedication1().getActiveIngredient());
        response.setActiveIngredient2(alert.getMedication2().getActiveIngredient());
        if (alert.getSourceName() != null && alert.getSourceName().contains("Mock")) {
            response.setEvidenceStatus("DEVELOPMENT_MOCK");
        } else {
            response.setEvidenceStatus("VERIFIED");
        }
        return response;
    }
}
