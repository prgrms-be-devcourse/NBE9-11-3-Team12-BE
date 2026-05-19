package com.rungo.api.global.infrastructure.mail.repository

import EmailOutboxStatus
import com.rungo.api.global.infrastructure.mail.entity.EmailOutbox
import org.springframework.data.jpa.repository.JpaRepository

interface EmailOutboxRepository : JpaRepository<EmailOutbox, Long> {

    fun findTop50ByStatusOrderByCreatedAtAsc(
        status: EmailOutboxStatus
    ): List<EmailOutbox>
}