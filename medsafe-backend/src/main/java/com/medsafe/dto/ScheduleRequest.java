package com.medsafe.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public class ScheduleRequest {

    @NotNull(message = "Scheduled time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime scheduledTime;

    @Size(max = 50, message = "Meal relation must not exceed 50 characters")
    private String mealRelation;

    public ScheduleRequest() {
    }

    public ScheduleRequest(LocalTime scheduledTime, String mealRelation) {
        this.scheduledTime = scheduledTime;
        this.mealRelation = mealRelation;
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
