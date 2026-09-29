package com.medsafe.dto;

import jakarta.validation.constraints.NotBlank;

public class AIExplanationRequest {

    @NotBlank(message = "drugA name is required")
    private String drugA;

    @NotBlank(message = "drugB name is required")
    private String drugB;

    private String severity;

    @NotBlank(message = "verifiedInformation is required - AI must be grounded in verified source data")
    private String verifiedInformation;

    @NotBlank(message = "source is required")
    private String source;

    private String evidenceStatus = "VERIFIED";

    public AIExplanationRequest() {
    }

    public AIExplanationRequest(String drugA, String drugB, String severity, String verifiedInformation, String source) {
        this.drugA = drugA;
        this.drugB = drugB;
        this.severity = severity;
        this.verifiedInformation = verifiedInformation;
        this.source = source;
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

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getVerifiedInformation() {
        return verifiedInformation;
    }

    public void setVerifiedInformation(String verifiedInformation) {
        this.verifiedInformation = verifiedInformation;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEvidenceStatus() {
        return evidenceStatus;
    }

    public void setEvidenceStatus(String evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }
}
