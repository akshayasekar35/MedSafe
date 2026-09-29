package com.medsafe.dto;

import java.util.List;

public class InteractionCheckRequest {
    private List<Long> medicationIds;

    public InteractionCheckRequest() {
    }

    public InteractionCheckRequest(List<Long> medicationIds) {
        this.medicationIds = medicationIds;
    }

    public List<Long> getMedicationIds() {
        return medicationIds;
    }

    public void setMedicationIds(List<Long> medicationIds) {
        this.medicationIds = medicationIds;
    }
}
