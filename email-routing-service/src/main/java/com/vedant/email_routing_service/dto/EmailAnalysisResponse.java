package com.vedant.email_routing_service.dto;

import lombok.*;


public class EmailAnalysisResponse {
    private String sentiment;
    private String forward_to;

    public EmailAnalysisResponse() {
    }

    public EmailAnalysisResponse(String sentiment, String forward_to) {
        this.sentiment = sentiment;
        this.forward_to = forward_to;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getForward_to() {
        return forward_to;
    }

    public void setForward_to(String forward_to) {
        this.forward_to = forward_to;
    }
}
