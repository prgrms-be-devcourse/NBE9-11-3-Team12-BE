package com.rungo.api.domain.notification.listener

import com.rungo.api.domain.notification.event.MarathonCanceledEvent
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.domain.notification.support.NotificationEmailFactory
import com.rungo.api.global.infrastructure.mail.EmailService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NotificationEventListener(
    private val emailService: EmailService,
    private val notificationEmailFactory: NotificationEmailFactory
) {

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleRegistrationCompleted(event: RegistrationCompletedEvent) {
        emailService.send(
            notificationEmailFactory.registrationCompleted(
                event.email,
                event.marathonTitle,
                event.courseName
            )
        )
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMarathonCanceled(event: MarathonCanceledEvent) {
        event.participantEmails.forEach { email ->
            emailService.send(
                notificationEmailFactory.marathonCanceled(email, event.marathonTitle)
            )
        }
    }
}