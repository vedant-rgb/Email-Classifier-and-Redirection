package com.vedant.email_routing_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Attachment implements Serializable {
    @JsonProperty("filename")
    private String filename;

    @JsonProperty("contentType")
    private String contentType;

    @JsonProperty("data")
    private String data; // Base64-encoded

    public Attachment() {
    }

    public Attachment(String filename, String contentType, String data) {
        this.filename = filename;
        this.contentType = contentType;
        this.data = data;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}