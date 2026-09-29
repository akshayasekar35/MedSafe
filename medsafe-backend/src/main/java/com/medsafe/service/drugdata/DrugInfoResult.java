package com.medsafe.service.drugdata;

import com.medsafe.service.drugdata.model.EvidenceStatus;
import java.util.ArrayList;
import java.util.List;

public class DrugInfoResult {
    private String standardName;
    private String activeIngredient;
    private String drugClass;
    private String externalSource;
    private String externalId;
    private EvidenceStatus evidenceStatus = EvidenceStatus.VERIFIED;
    private List<String> ambiguousCandidates = new ArrayList<>();

    public DrugInfoResult() {
    }

    public DrugInfoResult(String standardName, String activeIngredient, String drugClass, String externalSource, String externalId) {
        this.standardName = standardName;
        this.activeIngredient = activeIngredient;
        this.drugClass = drugClass;
        this.externalSource = externalSource;
        this.externalId = externalId;
        this.evidenceStatus = EvidenceStatus.VERIFIED;
    }

    public DrugInfoResult(String standardName, String activeIngredient, String drugClass, String externalSource, String externalId, EvidenceStatus status) {
        this.standardName = standardName;
        this.activeIngredient = activeIngredient;
        this.drugClass = drugClass;
        this.externalSource = externalSource;
        this.externalId = externalId;
        this.evidenceStatus = status;
    }

    public static DrugInfoResult ambiguous(String query, List<String> candidates) {
        DrugInfoResult res = new DrugInfoResult(query, null, null, "RxNorm", null, EvidenceStatus.AMBIGUOUS);
        res.setAmbiguousCandidates(candidates);
        return res;
    }

    public static DrugInfoResult notFound(String query) {
        return new DrugInfoResult(query, null, null, "RxNorm", null, EvidenceStatus.NOT_FOUND);
    }

    public static DrugInfoResult unavailable(String query) {
        return new DrugInfoResult(query, null, null, "RxNorm", null, EvidenceStatus.SOURCE_UNAVAILABLE);
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

    public EvidenceStatus getEvidenceStatus() {
        return evidenceStatus;
    }

    public void setEvidenceStatus(EvidenceStatus evidenceStatus) {
        this.evidenceStatus = evidenceStatus;
    }

    public List<String> getAmbiguousCandidates() {
        return ambiguousCandidates;
    }

    public void setAmbiguousCandidates(List<String> ambiguousCandidates) {
        this.ambiguousCandidates = ambiguousCandidates;
    }
}
