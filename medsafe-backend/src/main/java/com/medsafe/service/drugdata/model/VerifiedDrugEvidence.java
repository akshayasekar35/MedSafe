package com.medsafe.service.drugdata.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VerifiedDrugEvidence {

    private String drugNameA;
    private String drugNameB;
    private String normalizedNameA;
    private String normalizedNameB;
    private String activeIngredientA;
    private String activeIngredientB;
    private String externalIdA;
    private String externalIdB;
    private String interactionDescription;
    private String severity;
    private String sourceName;
    private String sourceReference;
    private LocalDateTime retrievedAt;
    private EvidenceStatus evidenceStatus;
    private List<String> ambiguousOptions = new ArrayList<>();
    private String safetyNotice;

    public VerifiedDrugEvidence() {
        this.retrievedAt = LocalDateTime.now();
        this.evidenceStatus = EvidenceStatus.NOT_FOUND;
    }

    public VerifiedDrugEvidence(String drugNameA, String drugNameB, EvidenceStatus evidenceStatus, String interactionDescription, String severity, String sourceName, String sourceReference) {
        this.drugNameA = drugNameA;
        this.drugNameB = drugNameB;
        this.evidenceStatus = evidenceStatus;
        this.interactionDescription = interactionDescription;
        this.severity = severity;
        this.sourceName = sourceName;
        this.sourceReference = sourceReference;
        this.retrievedAt = LocalDateTime.now();
    }

    public String getDrugNameA() {
        return drugNameA;
    }

    public void setDrugNameA(String drugNameA) {
        this.drugNameA = drugNameA;
    }

    public String getDrugNameB() {
        return drugNameB;
    }

    public void setDrugNameB(String drugNameB) {
        this.drugNameB = drugNameB;
    }

    public String getNormalizedNameA() {
        return normalizedNameA;
    }

    public void setNormalizedNameA(String normalizedNameA) {
        this.normalizedNameA = normalizedNameA;
    }

    public String getNormalizedNameB() {
        return normalizedNameB;
    }

    public void setNormalizedNameB(String normalizedNameB) {
        this.normalizedNameB = normalizedNameB;
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

    public String getExternalIdA() {
        return externalIdA;
    }

    public void setExternalIdA(String externalIdA) {
        this.externalIdA = externalIdA;
    }

    public String getExternalIdB() {
        return externalIdB;
    }

    public void setExternalIdB(String externalIdB) {
        this.externalIdB = externalIdB;
    }

    public String getInteractionDescription() {
        return interactionDescription;
    }

    public void setInteractionDescription(String interactionDescription) {
        this.interactionDescription = interactionDescription;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
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

    public LocalDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(LocalDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public EvidenceStatus getEvidenceStatus() {
        return evidenceStatus;
    }

    public void setEvidenceStatus(EvidenceStatus evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    public List<String> getAmbiguousOptions() {
        return ambiguousOptions;
    }

    public void setAmbiguousOptions(List<String> ambiguousOptions) {
        this.ambiguousOptions = ambiguousOptions;
    }

    public String getSafetyNotice() {
        return safetyNotice;
    }

    public void setSafetyNotice(String safetyNotice) {
        this.safetyNotice = safetyNotice;
    }
}
