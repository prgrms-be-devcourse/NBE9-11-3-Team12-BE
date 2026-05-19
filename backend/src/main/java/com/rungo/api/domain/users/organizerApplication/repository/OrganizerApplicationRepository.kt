package com.rungo.api.domain.users.organizerApplication.repository

import com.rungo.api.domain.users.organizerApplication.entity.OrganizerApplication
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizerApplicationRepository : JpaRepository<OrganizerApplication, Long>
