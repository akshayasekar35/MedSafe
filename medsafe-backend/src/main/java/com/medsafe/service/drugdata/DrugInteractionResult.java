package com.medsafe.service.drugdata;

import com.medsafe.service.drugdata.model.EvidenceStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DrugInteractionResult {
    private String drugA;
    private String drugB;
    private boolean hasInteraction;
    private String severity;
    private String description;
    private String sourceName;
    private String sourceReference;
    private EvidenceStatus evidenceStatus = EvidenceStatus.VERIFIED;
    private LocalDateTime retrievedAt = LocalDateTime.now();
    private List<String> ambiguousOptions = new ArrayList<>();
    private String rxcuiA;
    private String rxcuiB;
    private String activeIngredientA;
    private String activeIngredientB;
    private String standardNameA;
    private String standardNameB;

    public DrugInteractionResult() {
    }

    public DrugInteractionResult(String drugA, String drugB, boolean hasInteraction, String severity, String description, String sourceName, String sourceReference) {
        this.drugA = drugA;
        this.drugB = drugB;
        this.hasInteraction = hasInteraction;
        this.severity = severity;
        this.description = description;
        this.sourceName = sourceName;
        this.sourceReference = sourceReference;
        this.evidenceStatus = hasInteraction ? EvidenceStatus.VERIFIED : EvidenceStatus.NOT_FOUND;
        this.retrievedAt = LocalDateTime.now();
    }

    public DrugInteractionResult(String drugA, String drugB, boolean hasInteraction, String severity, String description, String sourceName, String sourceReference, EvidenceStatus evidenceStatus) {
        this.drugA = drugA;
        this.drugB = drugB;
        this.hasInteraction = hasInteraction;
        this.severity = severity;
        this.description = description;
        this.sourceName = sourceName;
        this.sourceReference = sourceReference;
        this.evidenceStatus = evidenceStatus;
        this.retrievedAt = LocalDateTime.now();
    }

    public static DrugInteractionResult notFound(String drugA, String drugB, String sourceName) {
        return new DrugInteractionResult(
                drugA,
                drugB,
                false,
                "NONE_IDENTIFIED",
                "No interaction identified in the consulted source.",
                sourceName != null ? sourceName : "Verified Drug Knowledgebase",
                "NOT_FOUND_IN_SOURCE",
                EvidenceStatus.NOT_FOUND
        );
    }

    public static DrugInteractionResult ambiguous(String drugA, String drugB, List<String> ambiguousOptions) {
        DrugInteractionResult res = new DrugInteractionResult(
                drugA,
                drugB,
                false,
                "AMBIGUOUS",
                "Please confirm the medicine before checking interactions.",
                "RxNorm Term Matching",
                "AMBIGUOUS_CONCEPT",
                EvidenceStatus.AMBIGUOUS
        );
        res.setAmbiguousOptions(ambiguousOptions);
        return res;
    }

    public static DrugInteractionResult sourceUnavailable(String drugA, String drugB, String sourceName) {
        return new DrugInteractionResult(
                drugA,
                drugB,
                false,
                "UNAVAILABLE",
                "External clinical source is temporarily unavailable. Verification could not be completed.",
                sourceName != null ? sourceName : "External Drug Source",
                "SERVICE_UNAVAILABLE",
                EvidenceStatus.SOURCE_UNAVAILABLE
        );
    }

    public String getDrugA() {
        return drugA;
    }

    public void setDrugA(String drugA) {
        this.drugA = drugA;
    }

    public String getDrugB() {
        return drugB;
    }

    public void setDrugB(String drugB) {
        this.drugB = drugB;
    }

    public boolean isHasInteraction() {
        return hasInteraction;
    }

    public void setHasInteraction(boolean hasInteraction) {
        this.hasInteraction = hasInteraction;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference;
    }

    public EvidenceStatus getEvidenceStatus() {
        return evidenceStatus;
    }

    public void setEvidenceStatus(EvidenceStatus evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    public LocalDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(LocalDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public List<String> getAmbiguousOptions() {
        return ambiguousOptions;
    }

    public void setAmbiguousOptions(List<String> ambiguousOptions) {
        this.ambiguousOptions = ambiguousOptions;
    }

    public String getRxcuiA() {
        return rxcuiA;
    }

    public void setRxcuiA(String rxcuiA) {
        this.rxcuiA = rxcuiA;
    }

    public String getRxcuiB() {
        return rxcuiB;
    }

    public void setRxcuiB(String rxcuiB) {
        this.rxcuiB = rxcuiB;
    }

    public String getActiveIngredientA() {
        return activeIngredientA;
    }

    public void setActiveIngredientA(String activeIngredientA) {
        this.activeIngredientA = activeIngredientA;
    }

    public String getActiveIngredientB() {
        return activeIngredientB;
    }

    public void setActiveIngredientB(String activeIngredientB) {
        this.activeIngredientB = activeIngredientB;
    }

    public String getStandardNameA() {
        return standardNameA;
    }

    public void setStandardNameA(String standardNameA) {
        this.standardNameA = standardNameA;
    }

    public String getStandardNameB() {
        return standardNameB;
    }

    public void setStandardNameB(String standardNameB) {
        this.standardNameB = standardNameB;
    }
}
