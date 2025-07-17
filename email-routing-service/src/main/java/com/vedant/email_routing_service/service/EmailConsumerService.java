package com.vedant.email_routing_service.service;

import com.vedant.email_routing_service.dto.EmailMessageDTO;
import org.jsoup.Jsoup;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EmailConsumerService {

    private final LangchainService langchainService;

    public EmailConsumerService(LangchainService langchainService) {
        this.langchainService = langchainService;
    }

    @KafkaListener(topics = "${kafka.topic.email}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(EmailMessageDTO email) {
        System.out.println("Received Email: " + email.getSubject());
        System.out.println("From: " + email.getFrom());
        System.out.println("Body: " + Jsoup.parse(email.getBody()).text());
        email.setBody(Jsoup.parse(email.getBody()).text()); // Clean HTML tags from body
        langchainService.analyzeAndRouteEmail(email);
    }
}
