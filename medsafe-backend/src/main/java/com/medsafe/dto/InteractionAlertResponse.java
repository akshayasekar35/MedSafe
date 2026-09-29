package com.medsafe.dto;

import java.time.LocalDateTime;

public class InteractionAlertResponse {
    private Long alertId;
    private Long medicationId1;
    private String medicineName1;
    private Long medicationId2;
    private String medicineName2;
    private String severity;
    private String description;
    private String sourceName;
    private String sourceReference;
    private LocalDateTime checkedAt;
    private String safetyNotice;
    private String evidenceStatus = "VERIFIED";
    private java.util.List<String> ambiguousOptions = new java.util.ArrayList<>();
    private String activeIngredient1;
    private String activeIngredient2;
    private String rxcui1;
    private String rxcui2;

    public InteractionAlertResponse() {
    }

    public InteractionAlertResponse(Long alertId, Long medicationId1, String medicineName1, Long medicationId2, String medicineName2, String severity, String description, String sourceName, String sourceReference, LocalDateTime checkedAt, String safetyNotice) {
        this.alertId = alertId;
        this.medicationId1 = medicationId1;
        this.medicineName1 = medicineName1;
        this.medicationId2 = medicationId2;
        this.medicineName2 = medicineName2;
        this.severity = severity;
        this.description = description;
        this.sourceName = sourceName;
        this.sourceReference = sourceReference;
        this.checkedAt = checkedAt;
        this.safetyNotice = safetyNotice;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public Long getMedicationId1() {
        return medicationId1;
    }

    public void setMedicationId1(Long medicationId1) {
        this.medicationId1 = medicationId1;
    }

    public String getMedicineName1() {
        return medicineName1;
    }

    public void setMedicineName1(String medicineName1) {
        this.medicineName1 = medicineName1;
    }

    public Long getMedicationId2() {
        return medicationId2;
    }

    public void setMedicationId2(Long medicationId2) {
        this.medicationId2 = medicationId2;
    }

    public String getMedicineName2() {
        return medicineName2;
    }

    public void setMedicineName2(String medicineName2) {
        this.medicineName2 = medicineName2;
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

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getSafetyNotice() {
        return safetyNotice;
    }

    public void setSafetyNotice(String safetyNotice) {
        this.safetyNotice = safetyNotice;
    }

    public String getEvidenceStatus() {
        return evidenceStatus;
    }

    public void setEvidenceStatus(String evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    public java.util.List<String> getAmbiguousOptions() {
        return ambiguousOptions;
    }

    public void setAmbiguousOptions(java.util.List<String> ambiguousOptions) {
        this.ambiguousOptions = ambiguousOptions;
    }

    public String getActiveIngredient1() {
        return activeIngredient1;
    }

    public void setActiveIngredient1(String activeIngredient1) {
        this.activeIngredient1 = activeIngredient1;
    }

    public String getActiveIngredient2() {
        return activeIngredient2;
    }

    public void setActiveIngredient2(String activeIngredient2) {
        this.activeIngredient2 = activeIngredient2;
    }

    public String getRxcui1() {
        return rxcui1;
    }

    public void setRxcui1(String rxcui1) {
        this.rxcui1 = rxcui1;
    }

    public String getRxcui2() {
        return rxcui2;
    }

    public void setRxcui2(String rxcui2) {
        this.rxcui2 = rxcui2;
    }
}
