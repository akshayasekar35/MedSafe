package com.medsafe.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class MedicationLogResponse {
    private Long logId;
    private Long scheduleId;
    private Long medicationId;
    private String medicineName;
    private LocalTime scheduledTime;
    private String mealRelation;
    private LocalDateTime scheduledDatetime;
    private LocalDateTime actualDatetime;
    private String status;

    public MedicationLogResponse() {
    }

    public MedicationLogResponse(Long logId, Long scheduleId, Long medicationId, String medicineName, LocalTime scheduledTime, String mealRelation, LocalDateTime scheduledDatetime, LocalDateTime actualDatetime, String status) {
        this.logId = logId;
        this.scheduleId = scheduleId;
        this.medicationId = medicationId;
        this.medicineName = medicineName;
        this.scheduledTime = scheduledTime;
        this.mealRelation = mealRelation;
        this.scheduledDatetime = scheduledDatetime;
        this.actualDatetime = actualDatetime;
        this.status = status;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
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

    public LocalDateTime getScheduledDatetime() {
        return scheduledDatetime;
    }

    public void setScheduledDatetime(LocalDateTime scheduledDatetime) {
        this.scheduledDatetime = scheduledDatetime;
    }

    public LocalDateTime getActualDatetime() {
        return actualDatetime;
    }

    public void setActualDatetime(LocalDateTime actualDatetime) {
        this.actualDatetime = actualDatetime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
