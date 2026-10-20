
package com.rkind.ecommerce.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        // Failure is logged and propagated to the caller.
        // The caller can then log the order-specific failure.
        mailSender.send(message);

        log.info(
                "Email accepted by SMTP server for recipient {} with subject {}",
                to,
                subject
        );
    }
}
