package com.medibook.notification.service.impl;

import com.medibook.notification.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String message) {
        if (to == null || to.isBlank()) {
            log.warn("Email recipient is empty. Skipping email send.");
            return;
        }

        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("spring.mail.username is not configured. Skipping email send.");
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(message);

            mailSender.send(mail);
            log.info("Email sent successfully to {}", to);

        } catch (MailException e) {
            // FIX: Log full details and re-throw so the service layer can record FAILED
            // status.
            // Previously this re-threw a generic Exception that would bubble up uncaught
            // and produce a 500 instead of saving a FAILED notification record.
            log.error("Failed to send email to {} - subject '{}': {}", to, subject, e.getMessage(), e);
            throw e; // Let NotificationServiceImpl catch this and set status=FAILED
        }
    }
}
