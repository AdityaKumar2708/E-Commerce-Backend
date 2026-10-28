package com.rkind.ecommerce.contact.controller;

import com.rkind.ecommerce.contact.request.ContactRequest;
import com.rkind.ecommerce.email.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final EmailService emailService;
    private final String recipientEmail;

    public ContactController(
            EmailService emailService,
            @Value("${app.admin.email}") String recipientEmail
    ) {
        this.emailService = emailService;
        this.recipientEmail = recipientEmail;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> submitContactForm(
            @Valid @RequestBody ContactRequest request
    ) {
        String subject = "New Website Enquiry - " + request.getName();

        String body = """
                New enquiry received from the website.

                Name: %s
                Email: %s
                Phone: %s

                Description:
                %s
                """.formatted(
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                request.getDescription()
        );

        try {
            emailService.sendEmail(recipientEmail, subject, body);

            return ResponseEntity.ok(Map.of(
                    "success", "true",
                    "message", "Your enquiry has been submitted successfully."
            ));

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to send your enquiry right now. Please try again later."
            );
        }
    }
}