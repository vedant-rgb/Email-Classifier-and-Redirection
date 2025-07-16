package com.vedant.email_fetcher_service.dto;

import java.io.Serializable;

public class Attachment implements Serializable {
    private String fileName;
    private String mimeType;
    private String base64Content;

    public Attachment(String fileName, String mimeType, String base64Content) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.base64Content = base64Content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getBase64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }
}