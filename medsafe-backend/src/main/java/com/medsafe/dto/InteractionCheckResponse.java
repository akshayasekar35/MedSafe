package com.medsafe.dto;

import java.util.List;

public class InteractionCheckResponse {
    private int totalInteractionsFound;
    private List<InteractionAlertResponse> alerts;
    private String safetySummary;
    private String disclaimer;

    public InteractionCheckResponse() {
    }

    public InteractionCheckResponse(int totalInteractionsFound, List<InteractionAlertResponse> alerts, String safetySummary, String disclaimer) {
        this.totalInteractionsFound = totalInteractionsFound;
        this.alerts = alerts;
        this.safetySummary = safetySummary;
        this.disclaimer = disclaimer;
    }

    public int getTotalInteractionsFound() {
        return totalInteractionsFound;
    }

    public void setTotalInteractionsFound(int totalInteractionsFound) {
        this.totalInteractionsFound = totalInteractionsFound;
    }

    public List<InteractionAlertResponse> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<InteractionAlertResponse> alerts) {
        this.alerts = alerts;
    }

    public String getSafetySummary() {
        return safetySummary;
    }

    public void setSafetySummary(String safetySummary) {
        this.safetySummary = safetySummary;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
