package com.medsafe.service;

import com.medsafe.dto.DuplicateWarningResponse;
import com.medsafe.entity.Medication;
import com.medsafe.repository.MedicationRepository;
import com.medsafe.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DuplicateDetectionService {

    private final MedicationRepository medicationRepository;
    private final SecurityUtils securityUtils;

    public DuplicateDetectionService(MedicationRepository medicationRepository, SecurityUtils securityUtils) {
        this.medicationRepository = medicationRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<DuplicateWarningResponse> detectDuplicateMedicationsForCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        List<Medication> activeMeds = medicationRepository.findByUserUserIdAndStatus(userId, "ACTIVE");

        // Map normalized ingredient -> list of medications
        Map<String, List<Medication>> ingredientMap = new LinkedHashMap<>();

        for (Medication med : activeMeds) {
            String ingredient = med.getActiveIngredient();
            if (ingredient == null || ingredient.trim().isEmpty()) {
                continue;
            }

            // Split on common multi-ingredient delimiters: comma, slash, plus
            String[] tokens = ingredient.split("[,/+]");
            for (String token : tokens) {
                String normalized = normalizeIngredient(token);
                if (!normalized.isEmpty()) {
                    ingredientMap.computeIfAbsent(normalized, k -> new ArrayList<>()).add(med);
                }
            }
        }

        List<DuplicateWarningResponse> warnings = new ArrayList<>();

        for (Map.Entry<String, List<Medication>> entry : ingredientMap.entrySet()) {
            List<Medication> meds = entry.getValue();
            // Deduplicate medications list in case a single medication matched multiple tokens
            List<Medication> distinctMeds = meds.stream().distinct().collect(Collectors.toList());

            if (distinctMeds.size() > 1) {
                String ingredientName = capitalizeWords(entry.getKey());
                List<DuplicateWarningResponse.MedicationSummary> summaries = distinctMeds.stream()
                        .map(m -> new DuplicateWarningResponse.MedicationSummary(
                                m.getMedicationId(),
                                m.getMedicineName(),
                                m.getDosage(),
                                m.getFrequency()))
                        .collect(Collectors.toList());

                String warning = "Warning: " + distinctMeds.size() + " of your active medications contain the same active ingredient ('"
                        + ingredientName + "'). Taking multiple medications sharing the same active ingredient may lead to unintended additive dosage or toxicity.";

                String recommendation = "Review these medications with your physician or pharmacist. Do not stop or alter your prescribed medication regimen without professional healthcare guidance.";

                warnings.add(new DuplicateWarningResponse(ingredientName, summaries, warning, recommendation));
            }
        }

        return warnings;
    }

    private String normalizeIngredient(String s) {
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
