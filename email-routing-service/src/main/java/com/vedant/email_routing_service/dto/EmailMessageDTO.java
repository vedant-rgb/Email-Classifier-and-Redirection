package com.vedant.email_routing_service.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data

public class EmailMessageDTO implements Serializable {
    private String subject;
    private String from;
    private String body;
    private List<Attachment> attachments;

    public EmailMessageDTO() {
        this.attachments = new ArrayList<>();
    }

    public EmailMessageDTO(String subject, String from, String body) {
        this.subject = subject;
        this.from = from;
        this.body = body;
        this.attachments = new ArrayList<>();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
    }


}