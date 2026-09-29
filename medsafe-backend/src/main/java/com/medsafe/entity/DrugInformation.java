package com.medsafe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "drug_information")
public class DrugInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "drug_id")
    private Long drugId;

    @Column(name = "standard_name", nullable = false, length = 200)
    private String standardName;

    @Column(name = "active_ingredient", length = 300)
    private String activeIngredient;

    @Column(name = "drug_class", length = 200)
    private String drugClass;

    @Column(name = "external_source", length = 200)
    private String externalSource;

    @Column(name = "external_id", length = 200)
    private String externalId;

    @Column(name = "last_updated", insertable = false, updatable = false)
    private LocalDateTime lastUpdated;

    public DrugInformation() {
    }

    public DrugInformation(String standardName, String activeIngredient, String drugClass, String externalSource, String externalId) {
        this.standardName = standardName;
        this.activeIngredient = activeIngredient;
        this.drugClass = drugClass;
        this.externalSource = externalSource;
        this.externalId = externalId;
    }

    public Long getDrugId() {
        return drugId;
    }

    public void setDrugId(Long drugId) {
        this.drugId = drugId;
    }

    public String getStandardName() {
        return standardName;
    }

    public void setStandardName(String standardName) {
        this.standardName = standardName;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    public String getDrugClass() {
        return drugClass;
    }

    public void setDrugClass(String drugClass) {
        this.drugClass = drugClass;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
