package com.rungo.api.global.infrastructure.mail.repository

import com.rungo.api.global.infrastructure.mail.entity.EmailOutbox
import EmailOutboxStatus
import org.springframework.data.jpa.repository.JpaRepository

interface EmailOutboxRepository : JpaRepository<EmailOutbox, Long> {

    fun findTop50ByStatusInOrderByCreatedAtAsc(
        statuses: Collection<EmailOutboxStatus>
    ): List<EmailOutbox>
}