package com.rungo.api.domain.marathon.marathon.repository

import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface MarathonRepository : JpaRepository<Marathon, Long> {

    fun findByStatusIn(
        statuses: Collection<MarathonStatus>,
        pageable: Pageable,
    ): Page<Marathon>

    fun findAllByTitleStartingWith(prefix: String): List<Marathon>

    @EntityGraph(attributePaths = ["courses"])
    fun findByIdAndOrganizer_Id(
        marathonId: Long,
        organizerId: Long,
    ): Marathon?

    @EntityGraph(attributePaths = ["courses"])
    fun findByOrganizerIdAndStatusNotIn(
        organizerId: Long,
        statuses: Collection<MarathonStatus>,
    ): List<Marathon>

    fun findByOrganizerId(organizerId: Long): List<Marathon>

    fun findAllByEventDateBefore(date: LocalDate): List<Marathon>

    @Query("SELECT m.id FROM Marathon m WHERE m.eventDate < :date")
    fun findIdsByEventDateBefore(@Param("date") date: LocalDate): List<Long>
}
