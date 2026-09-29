package com.medsafe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medication_logs")
public class MedicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "scheduled_datetime", nullable = false)
    private LocalDateTime scheduledDatetime;

    @Column(name = "actual_datetime")
    private LocalDateTime actualDatetime;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    public MedicationLog() {
    }

    public MedicationLog(Schedule schedule, LocalDateTime scheduledDatetime, LocalDateTime actualDatetime, String status) {
        this.schedule = schedule;
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

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
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
