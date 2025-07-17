package com.vedant.email_routing_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vedant.email_routing_service.dto.EmailAnalysisResponse;
import com.vedant.email_routing_service.dto.EmailMessageDTO;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Service
public class LangchainService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_EMAILS = Set.of(
            "alice.j@yourcompany.com", "rajiv.m@yourcompany.com",
            "sarah.c@yourcompany.com", "ankit.s@yourcompany.com",
            "priya.p@yourcompany.com", "john.l@yourcompany.com",
            "angela.w@yourcompany.com", "kevin.w@yourcompany.com",
            "rohit.n@yourcompany.com", "julia.c@yourcompany.com",
            "tanya.s@yourcompany.com", "david.k@yourcompany.com",
            "abdul.r@yourcompany.com", "neha.v@yourcompany.com",
            "aditya.r@yourcompany.com", "zoe.y@yourcompany.com"
    );

    public LangchainService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void analyzeAndRouteEmail(EmailMessageDTO email) {
        String url = "http://localhost:8000/analyze_email/";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EmailMessageDTO> request = new HttpEntity<>(email, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            String responseBody = response.getBody();

            System.out.println("LangChain Response: " + responseBody);

            // Parse JSON → extract "analysis" string → map to POJO
            JsonNode root = objectMapper.readTree(responseBody);
            String analysisString = root.path("analysis").asText();

            // Clean possible triple backticks or json formatting
            String cleaned = analysisString.replace("```json", "").replace("```", "").trim();

            EmailAnalysisResponse analysis = objectMapper.readValue(cleaned, EmailAnalysisResponse.class);

            System.out.println("Sentiment: " + analysis.getSentiment());
            System.out.println("Forward To: " + analysis.getForward_to());

            if (ALLOWED_EMAILS.contains(analysis.getForward_to())) {
                System.out.println("✅ Forwarding email to: " + analysis.getForward_to());
                // TODO: Add your forwarding logic here
            } else {
                System.out.println("❌ Email address not found in RAG: " + analysis.getForward_to());
            }

        } catch (Exception e) {
            System.err.println("LangChain call failed: " + e.getMessage());
        }
    }
}
