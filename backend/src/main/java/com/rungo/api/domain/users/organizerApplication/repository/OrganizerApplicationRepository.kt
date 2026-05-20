package com.rungo.api.domain.users.organizerApplication.repository

import com.rungo.api.domain.users.organizerApplication.entity.OrganizerApplication
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizerApplicationRepository : JpaRepository<OrganizerApplication, Long>{
    fun existsByUserIdAndStatus(userId: Long, status: ApplicationStatus): Boolean


    @EntityGraph(attributePaths = ["user"])
    override fun findAll(pageable: Pageable): Page<OrganizerApplication>

    @EntityGraph(attributePaths = ["user"])
    fun findAllByStatus(
        status: ApplicationStatus,
        pageable: Pageable,
    ): Page<OrganizerApplication>
}