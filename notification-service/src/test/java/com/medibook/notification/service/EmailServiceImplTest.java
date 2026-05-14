package com.medibook.notification.service;

import com.medibook.notification.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@medibook.com");
    }

    @Test
    void sendEmail_success() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService.sendEmail("user@example.com", "Subject", "Message"))
                .doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_emptyRecipient_skipsWithoutThrow() {
        assertThatCode(() -> emailService.sendEmail("", "Subject", "Message"))
                .doesNotThrowAnyException();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_nullRecipient_skipsWithoutThrow() {
        assertThatCode(() -> emailService.sendEmail(null, "Subject", "Message"))
                .doesNotThrowAnyException();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_fromEmailNotConfigured_skipsWithoutThrow() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "");

        assertThatCode(() -> emailService.sendEmail("user@example.com", "Subject", "Message"))
                .doesNotThrowAnyException();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_smtpFailure_rethrowsMailException() {
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail("user@example.com", "Subject", "Message"))
                .isInstanceOf(MailSendException.class);
    }
}
