package com.medsafe.dto;

public class AIExplanationResponse {
    private String drugA;
    private String drugB;
    private String severity;
    private String patientFriendlyExplanation;
    private String verifiedSource;
    private String safetyNotice;
    private String disclaimer;

    public AIExplanationResponse() {
    }

    public AIExplanationResponse(String drugA, String drugB, String severity, String patientFriendlyExplanation, String verifiedSource, String safetyNotice, String disclaimer) {
        this.drugA = drugA;
        this.drugB = drugB;
        this.severity = severity;
        this.patientFriendlyExplanation = patientFriendlyExplanation;
        this.verifiedSource = verifiedSource;
        this.safetyNotice = safetyNotice;
        this.disclaimer = disclaimer;
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

    public String getPatientFriendlyExplanation() {
        return patientFriendlyExplanation;
    }

    public void setPatientFriendlyExplanation(String patientFriendlyExplanation) {
        this.patientFriendlyExplanation = patientFriendlyExplanation;
    }

    public String getVerifiedSource() {
        return verifiedSource;
    }

    public void setVerifiedSource(String verifiedSource) {
        this.verifiedSource = verifiedSource;
    }

    public String getSafetyNotice() {
        return safetyNotice;
    }

    public void setSafetyNotice(String safetyNotice) {
        this.safetyNotice = safetyNotice;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
