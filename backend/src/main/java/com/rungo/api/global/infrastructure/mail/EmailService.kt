package com.rungo.api.global.infrastructure.mail

import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSenderClient: EmailSenderClient
) {

    fun send(emailMessage: EmailMessage) {
        mailSenderClient.send(emailMessage)
    }

    fun sendEmail(to: String, subject: String, body: String) {
        send(EmailMessage(to, subject, body))
    }
}