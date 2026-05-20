package com.rungo.api.domain.notification.listener

import com.rungo.api.domain.notification.event.MarathonCanceledEvent
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent
import com.rungo.api.domain.notification.support.NotificationEmailFactory
import com.rungo.api.global.infrastructure.mail.EmailMessage
import com.rungo.api.global.infrastructure.mail.entity.EmailOutbox
import com.rungo.api.global.infrastructure.mail.repository.EmailOutboxRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NotificationEventListener(
    private val notificationEmailFactory: NotificationEmailFactory,
    private val emailOutboxRepository: EmailOutboxRepository,
) {

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleRegistrationCompleted(event: RegistrationCompletedEvent) {
        val message = notificationEmailFactory.registrationCompleted(
            event.email,
            event.marathonTitle,
            event.courseName,
        )

        saveEmailOutbox(message)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleMarathonCanceled(event: MarathonCanceledEvent) {
        event.participantEmails
            .map { email ->
                notificationEmailFactory.marathonCanceled(
                    email,
                    event.marathonTitle,
                )
            }
            .forEach(::saveEmailOutbox)
    }

    private fun saveEmailOutbox(message: EmailMessage) {
        emailOutboxRepository.save(
            EmailOutbox.create(
                recipient = message.to,
                subject = message.subject,
                body = message.body,
            )
        )
    }
}