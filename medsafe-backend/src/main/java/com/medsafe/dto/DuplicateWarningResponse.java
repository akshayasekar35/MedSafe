package com.medsafe.dto;

import java.util.List;

public class DuplicateWarningResponse {
    private String activeIngredient;
    private List<MedicationSummary> medications;
    private String warningMessage;
    private String clinicalRecommendation;

    public DuplicateWarningResponse() {
    }

    public DuplicateWarningResponse(String activeIngredient, List<MedicationSummary> medications, String warningMessage, String clinicalRecommendation) {
        this.activeIngredient = activeIngredient;
        this.medications = medications;
        this.warningMessage = warningMessage;
        this.clinicalRecommendation = clinicalRecommendation;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    public List<MedicationSummary> getMedications() {
        return medications;
    }

    public void setMedications(List<MedicationSummary> medications) {
        this.medications = medications;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }

    public String getClinicalRecommendation() {
        return clinicalRecommendation;
    }

    public void setClinicalRecommendation(String clinicalRecommendation) {
        this.clinicalRecommendation = clinicalRecommendation;
    }

    public static class MedicationSummary {
        private Long medicationId;
        private String medicineName;
        private String dosage;
        private String frequency;

        public MedicationSummary() {
        }

        public MedicationSummary(Long medicationId, String medicineName, String dosage, String frequency) {
            this.medicationId = medicationId;
            this.medicineName = medicineName;
            this.dosage = dosage;
            this.frequency = frequency;
        }

        public Long getMedicationId() {
            return medicationId;
        }

        public void setMedicationId(Long medicationId) {
            this.medicationId = medicationId;
        }

        public String getMedicineName() {
            return medicineName;
        }

        public void setMedicineName(String medicineName) {
            this.medicineName = medicineName;
        }

        public String getDosage() {
            return dosage;
        }

        public void setDosage(String dosage) {
            this.dosage = dosage;
        }

        public String getFrequency() {
            return frequency;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }
    }
}
