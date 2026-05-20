package com.rungo.api.domain.registration.repository

import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RegistrationCancelHistoryRepository : JpaRepository<RegistrationCancelHistory, Long> {
    fun findByUserId(userId: Long, pageable: Pageable): Page<RegistrationCancelHistory>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RegistrationCancelHistory h WHERE h.marathonId IN :marathonIds")
    fun deleteAllByMarathonIdIn(@Param("marathonIds") marathonIds: List<Long>)
}
