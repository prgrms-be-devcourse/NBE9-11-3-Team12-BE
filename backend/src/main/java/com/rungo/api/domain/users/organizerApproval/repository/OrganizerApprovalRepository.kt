package com.rungo.api.domain.users.organizerApproval.repository

import com.rungo.api.domain.users.organizerApproval.entity.OrganizerApproval
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizerApprovalRepository : JpaRepository<OrganizerApproval?, Long?>
