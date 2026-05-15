package com.rungo.api.domain.registration.repository

import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface RegistrationCancelHistoryRepository : JpaRepository<RegistrationCancelHistory, Long> {
    fun findByUserId(userId: Long, pageable: Pageable): Page<RegistrationCancelHistory>
}
