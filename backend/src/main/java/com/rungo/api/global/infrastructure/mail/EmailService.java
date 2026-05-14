package com.rungo.api.global.infrastructure.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailSenderClient mailSenderClient;

    public void send(EmailMessage emailMessage) {
        mailSenderClient.send(emailMessage);
    }

    public void sendEmail(String to, String subject, String body) {
        send(new EmailMessage(to, subject, body));
    }
}