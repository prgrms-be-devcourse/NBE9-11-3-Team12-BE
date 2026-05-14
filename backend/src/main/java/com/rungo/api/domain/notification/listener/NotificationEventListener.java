package com.rungo.api.domain.notification.listener;

import com.rungo.api.domain.notification.event.MarathonCanceledEvent;
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent;
import com.rungo.api.domain.notification.support.NotificationEmailFactory;
import com.rungo.api.global.infrastructure.mail.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;
    private final NotificationEmailFactory notificationEmailFactory;

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegistrationCompleted(RegistrationCompletedEvent event) {
        emailService.send(
                notificationEmailFactory.registrationCompleted(
                        event.email(),
                        event.marathonTitle(),
                        event.courseName()
                )
        );
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMarathonCanceled(MarathonCanceledEvent event) {
        for (String email : event.participantEmails()) {
            emailService.send(
                    notificationEmailFactory.marathonCanceled(email, event.marathonTitle())
            );
        }
    }
}