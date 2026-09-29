package com.medsafe.service;

import com.medsafe.dto.AIExplanationRequest;
import com.medsafe.dto.AIExplanationResponse;
import com.medsafe.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Evidence-Grounded AI Explanation Service.
 * 
 * Strict Safety Rules Enforced:
 * 1. AI explains ONLY retrieved verified information.
 * 2. Uses patient-friendly, understandable language.
 * 3. Preserves severity and explicitly attributes the verified source.
 * 4. Never invents drug interactions or alters prescriptions.
 * 5. Explicitly advises consulting a healthcare professional and warns against abruptly stopping medicines.
 */
@Service
public class DefaultAIExplanationService implements AIExplanationService {

    public static final String SAFETY_DISCLAIMER =
            "Medical Disclaimer: This explanation is generated using evidence-grounded AI based solely on verified data from %s. " +
            "It is provided for informational and educational purposes only and is not medical advice, diagnosis, or prescription guidance. " +
            "Do NOT stop or change your medication without consulting your doctor or pharmacist.";

    @Value("${app.ai.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Override
    public AIExplanationResponse explainInteraction(AIExplanationRequest request) {
        validateRequest(request);

        String drugA = request.getDrugA().trim();
        String drugB = request.getDrugB().trim();
        String status = request.getEvidenceStatus() != null ? request.getEvidenceStatus().trim().toUpperCase() : "VERIFIED";
        String verifiedInfo = request.getVerifiedInformation().trim();
        String source = request.getSource().trim();

        // 1. Guard against development/mock data being passed to AI explanation
        if ("DEVELOPMENT_MOCK".equalsIgnoreCase(status) || source.toLowerCase().contains("mock") || source.toLowerCase().contains("simulated")) {
            throw new BadRequestException("Mock development data cannot be presented or explained as verified clinical evidence. AI explanations require authentic clinical sources.");
        }

        // 2. Handlers for non-verified states (AI must NEVER invent clinical data or claim 100% safety)
        if ("NOT_FOUND".equals(status) || verifiedInfo.toLowerCase().contains("no interaction identified")) {
            String explanation = "No interaction identified in the consulted source (" + source + ").\n\n" +
                    "Important Clinical Safety Notice: The absence of a documented interaction in this database does NOT mean these medicines are completely safe or free from risk. " +
                    "Always consult your doctor or pharmacist before starting, stopping, or combining any medication.";
            String patientAdvice = "Absence of documented interaction is not a guarantee of safety. Discuss your combination with a healthcare provider.";
            String disclaimer = "Medical Disclaimer: Absence of a documented interaction in the consulted reference source does NOT guarantee the complete absence of all potential risks. Always consult your healthcare provider.";
            return new AIExplanationResponse(drugA, drugB, "NONE_IDENTIFIED", explanation, source, patientAdvice, disclaimer);
        }

        if ("AMBIGUOUS".equals(status) || verifiedInfo.toLowerCase().contains("confirm the medicine")) {
            String explanation = "The medication name '" + drugA + "' or '" + drugB + "' is ambiguous and could match multiple clinical formulations or concepts. " +
                    "A clinical safety explanation cannot be generated until the exact medication is confirmed. Please confirm the medicine before checking interactions.";
            String patientAdvice = "Please confirm the medicine before checking interactions.";
            String disclaimer = "Medical Disclaimer: Clarification of medicine name is required before interaction safety can be evaluated.";
            return new AIExplanationResponse(drugA, drugB, "AMBIGUOUS", explanation, source, patientAdvice, disclaimer);
        }

        if ("SOURCE_UNAVAILABLE".equals(status) || verifiedInfo.toLowerCase().contains("unavailable")) {
            String explanation = "External clinical verification could not be completed because the reference source is temporarily unavailable. " +
                    "Please verify interaction safety with your prescribing doctor or dispensing pharmacist.";
            String patientAdvice = "Verification could not be completed at this time.";
            String disclaimer = "Medical Disclaimer: Clinical source unavailable. Always consult a healthcare provider.";
            return new AIExplanationResponse(drugA, drugB, "UNAVAILABLE", explanation, source, patientAdvice, disclaimer);
        }

        // 3. Handlers for verified interaction evidence
        String severity = (request.getSeverity() != null && !request.getSeverity().isBlank())
                ? request.getSeverity().trim().toUpperCase() : "DOCUMENTED_IN_LABEL";

        String friendlyExplanation = buildPatientFriendlyExplanation(drugA, drugB, severity, verifiedInfo, source);
        String patientAdvice = "Please speak with your prescribing physician or pharmacist about taking " + drugA + " and " + drugB + " together. Do not discontinue your medication on your own.";
        String disclaimer = String.format(SAFETY_DISCLAIMER, source);

        return new AIExplanationResponse(
                drugA,
                drugB,
                severity,
                friendlyExplanation,
                source,
                patientAdvice,
                disclaimer
        );
    }

    private void validateRequest(AIExplanationRequest request) {
        if (request == null) {
            throw new BadRequestException("AI explanation request cannot be empty.");
        }
        if (request.getDrugA() == null || request.getDrugA().isBlank()) {
            throw new BadRequestException("Drug A must be specified.");
        }
        if (request.getDrugB() == null || request.getDrugB().isBlank()) {
            throw new BadRequestException("Drug B must be specified.");
        }
        if (request.getDrugA().trim().equalsIgnoreCase(request.getDrugB().trim())) {
            throw new BadRequestException("Cannot explain pairwise interactions for a medication with itself.");
        }
        if (request.getVerifiedInformation() == null || request.getVerifiedInformation().isBlank()) {
            throw new BadRequestException("AI explanation requires verified drug information from a trusted source. Unverified queries are prohibited.");
        }
        if (request.getSource() == null || request.getSource().isBlank()) {
            throw new BadRequestException("Source attribution is required for evidence grounding.");
        }
    }

    private String buildPatientFriendlyExplanation(String drugA, String drugB, String severity, String verifiedInfo, String source) {
        StringBuilder sb = new StringBuilder();

        boolean hasExplicitSeverity = !"DOCUMENTED_IN_LABEL".equalsIgnoreCase(severity) 
                && !"DOCUMENTED".equalsIgnoreCase(severity) 
                && severity != null;

        if (hasExplicitSeverity) {
            sb.append("Verified Summary (Severity: ").append(severity).append("):\n");
        } else {
            sb.append("Verified Summary (Severity: Not Specified by Source):\n");
        }

        sb.append("According to official records provided by ").append(source).append(", taking ")
                .append(drugA).append(" alongside ").append(drugB).append(" requires medical care because: ")
                .append(verifiedInfo).append("\n\n");

        sb.append("In Simple Terms:\n");
        // Simplify common clinical terms for patient readability
        String simplified = verifiedInfo
                .replaceAll("(?i)\\bconcurrent use\\b", "taking both together")
                .replaceAll("(?i)\\banticoagulant\\b", "blood-thinning medicine")
                .replaceAll("(?i)\\bantiplatelet\\b", "platelet-blocking medicine")
                .replaceAll("(?i)\\bhyperkalemia\\b", "dangerously high potassium levels in your bloodstream")
                .replaceAll("(?i)\\bmyopathy\\b", "muscle weakness and pain")
                .replaceAll("(?i)\\brhabdomyolysis\\b", "severe muscle breakdown")
                .replaceAll("(?i)\\bpharmacodynamic effects\\b", "the combined ways these drugs act inside your body")
                .replaceAll("(?i)\\bdiminish the antihypertensive effect\\b", "reduce the blood pressure lowering effect")
                .replaceAll("(?i)\\brenal impairment\\b", "strain or reduced kidney function");

        sb.append("When you take ").append(drugA).append(" and ").append(drugB).append(", ")
                .append(simplified);

        if (hasExplicitSeverity) {
            sb.append(" This interaction is classified at the '").append(severity).append("' level.\n\n");
        } else {
            sb.append(" This interaction is documented in official product labeling without an assigned discrete severity tier.\n\n");
        }

        sb.append("What You Should Do:\n");
        sb.append("1. Do not abruptly stop taking either medicine without speaking to your doctor.\n");
        sb.append("2. Inform your doctor or pharmacist that you are taking both ").append(drugA).append(" and ").append(drugB).append(".\n");
        sb.append("3. Watch for any unusual symptoms described by your healthcare team.");

        return sb.toString();
    }
}
