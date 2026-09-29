package com.medsafe.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

public class ScheduleResponse {
    private Long scheduleId;
    private Long medicationId;
    private String medicineName;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime scheduledTime;

    private String mealRelation;

    public ScheduleResponse() {
    }

    public ScheduleResponse(Long scheduleId, Long medicationId, String medicineName, LocalTime scheduledTime, String mealRelation) {
        this.scheduleId = scheduleId;
        this.medicationId = medicationId;
        this.medicineName = medicineName;
        this.scheduledTime = scheduledTime;
        this.mealRelation = mealRelation;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
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

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getMealRelation() {
        return mealRelation;
    }

    public void setMealRelation(String mealRelation) {
        this.mealRelation = mealRelation;
    }
}
