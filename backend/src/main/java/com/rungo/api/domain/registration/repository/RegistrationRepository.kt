package com.rungo.api.domain.registration.repository

import com.rungo.api.domain.registration.entity.Registration
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RegistrationRepository : JpaRepository<Registration, Long> {
    @EntityGraph(attributePaths = ["marathon", "course"])
    fun findByUser_Id(userId: Long, pageable: Pageable): Page<Registration>

//    @EntityGraph(attributePaths = {"marathon", "course"})
//    Page<Registration> findByUser_IdAndStatus(Long userId, RegistrationStatus status, Pageable pageable);
    @EntityGraph(attributePaths = ["user", "course", "marathon"])
    fun findAllByMarathon_IdOrderByAppliedAtDesc(marathonId: Long): List<Registration>

    @EntityGraph(attributePaths = ["course"])
    fun findByMarathon_Id(marathonId: Long, pageable: Pageable): Page<Registration>

    @EntityGraph(attributePaths = ["course"])
    fun findByMarathon_IdAndCourse_Id(marathonId: Long, courseId: Long, pageable: Pageable): Page<Registration>

    @EntityGraph(attributePaths = ["course"])
    fun findByMarathon_IdAndSnapNameContaining(
        marathonId: Long,
        name: String,
        pageable: Pageable
    ): Page<Registration>

    @EntityGraph(attributePaths = ["course"])
    fun findByMarathon_IdAndCourse_IdAndSnapNameContaining(
        marathonId: Long,
        courseId: Long,
        name: String,
        pageable: Pageable
    ): Page<Registration>

    @EntityGraph(attributePaths = ["marathon", "course"])
    fun findByIdAndMarathon_Id(registrationId: Long, marathonId: Long): Registration?

    @Query(
        """
    SELECT DISTINCT r.user.email
    FROM Registration r
    WHERE r.course.marathon.id = :marathonId
    """
    )
    fun findParticipantEmailsByMarathonId(@Param("marathonId") marathonId: Long): List<String>

    @Modifying
    @Query("DELETE FROM Registration r WHERE r.marathon.id IN :marathonIds")
    fun deleteAllByMarathonIdIn(@Param("marathonIds") marathonIds: List<Long>)
}
