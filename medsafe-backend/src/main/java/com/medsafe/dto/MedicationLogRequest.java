package com.medsafe.dto;

import java.time.LocalDateTime;

public class MedicationLogRequest {

    private LocalDateTime scheduledDatetime;

    public MedicationLogRequest() {
    }

    public MedicationLogRequest(LocalDateTime scheduledDatetime) {
        this.scheduledDatetime = scheduledDatetime;
    }

    public LocalDateTime getScheduledDatetime() {
        return scheduledDatetime;
    }

    public void setScheduledDatetime(LocalDateTime scheduledDatetime) {
        this.scheduledDatetime = scheduledDatetime;
    }
}
