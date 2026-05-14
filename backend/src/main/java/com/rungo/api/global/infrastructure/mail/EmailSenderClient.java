package com.rungo.api.global.infrastructure.mail;

import com.rungo.api.global.infrastructure.mail.exception.EmailSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSenderClient {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Retryable(
            retryFor = EmailSendException.class,
            maxAttemptsExpression = "${spring.mail.retry.max-attempts:3}",
            backoff = @Backoff(
                    delayExpression = "${spring.mail.retry.delay:1000}",
                    multiplierExpression = "${spring.mail.retry.multiplier:2.0}"
            )
    )
    public void send(EmailMessage emailMessage) {
        if (!mailEnabled) {
            log.info("메일 전송 비활성화 상태 - skip: to={}, subject={}",
                    emailMessage.to(), emailMessage.subject());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailMessage.to());
            message.setSubject(emailMessage.subject());
            message.setText(emailMessage.body());
            message.setFrom(fromEmail);

            mailSender.send(message);

            log.info("이메일 발송 성공: to={}, subject={}", emailMessage.to(), emailMessage.subject());
        } catch (MailException ex) {
            log.warn("이메일 발송 실패 - 재시도 예정: to={}, subject={}",
                    emailMessage.to(), emailMessage.subject(), ex);
            throw new EmailSendException("이메일 발송 실패", ex);
        }
    }

    @Recover
    public void recover(EmailSendException ex, EmailMessage emailMessage) {
        log.error("이메일 최종 발송 실패: to={}, subject={}",
                emailMessage.to(), emailMessage.subject(), ex);
    }
}