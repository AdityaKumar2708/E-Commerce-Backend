package com.rkind.ecommerce.email;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private static final String RESEND_API_URL =
            "https://api.resend.com/emails";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String fromEmail;

    public EmailService(
            ObjectMapper objectMapper,
            @Value("${resend.ecommerce.api-key}") String apiKey,
            @Value("${resend.ecommerce.from-email}") String fromEmail
    ) {
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
    }

    public void sendEmail(
            String to,
            String subject,
            String body
    ) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException(
                    "Customer email address is missing."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Resend E-Commerce API key is not configured."
            );
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException(
                    "Resend sender email is not configured."
            );
        }

        Map<String, Object> payload = Map.of(
                "from", fromEmail,
                "to", new String[]{to},
                "subject", subject == null ? "" : subject,
                "text", body == null ? "" : body
        );

        try {
            String response = restClient.post()
                    .uri(RESEND_API_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.info(
                    "Email accepted by Resend for recipient {} with subject {}. Response: {}",
                    to,
                    subject,
                    response
            );

        } catch (RestClientException ex) {
            log.error(
                    "Failed to send email through Resend to {} with subject {}",
                    to,
                    subject,
                    ex
            );

            throw new IllegalStateException(
                    "Failed to send email through Resend.",
                    ex
            );
        }
    }
}
