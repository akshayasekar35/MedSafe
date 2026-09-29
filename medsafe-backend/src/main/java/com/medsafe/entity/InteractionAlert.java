package com.medsafe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interaction_alerts")
public class InteractionAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_id_1", nullable = false)
    private Medication medication1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medication_id_2", nullable = false)
    private Medication medication2;

    @Column(name = "severity", length = 50)
    private String severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_name", length = 200)
    private String sourceName;

    @Column(name = "source_reference", length = 500)
    private String sourceReference;

    @Column(name = "checked_at", insertable = false, updatable = false)
    private LocalDateTime checkedAt;

    public InteractionAlert() {
    }

    public InteractionAlert(Medication medication1, Medication medication2, String severity, String description, String sourceName, String sourceReference) {
        this.medication1 = medication1;
        this.medication2 = medication2;
        this.severity = severity;
        this.description = description;
        this.sourceName = sourceName;
        this.sourceReference = sourceReference;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public Medication getMedication1() {
        return medication1;
    }

    public void setMedication1(Medication medication1) {
        this.medication1 = medication1;
    }

    public Medication getMedication2() {
        return medication2;
    }

    public void setMedication2(Medication medication2) {
        this.medication2 = medication2;
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
}
