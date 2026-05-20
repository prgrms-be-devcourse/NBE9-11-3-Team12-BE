package com.rungo.api.domain.registration.repository

import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
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

    // --------------------------------------------------------------------------
    // 아래 메서드는 위 메서드에 상태 추가한 코드, 위 메서드는 테스트용으로 남겨둠

    @EntityGraph(attributePaths = ["course"])
    fun findByMarathon_IdAndStatus(
        marathonId: Long,
        status: RegistrationStatus,
        pageable: Pageable,
    ): Page<Registration>

    @EntityGraph(attributePaths = ["course"])
    fun findByMarathon_IdAndCourse_IdAndStatus(
        marathonId: Long,
        courseId: Long,
        status: RegistrationStatus,
        pageable: Pageable,
    ): Page<Registration>

    @EntityGraph(attributePaths = ["course"])
    fun findByMarathon_IdAndSnapNameContainingAndStatus(
        marathonId: Long,
        name: String,
        status: RegistrationStatus,
        pageable: Pageable,
    ): Page<Registration>

    @EntityGraph(attributePaths = ["course"])
    fun findByMarathon_IdAndCourse_IdAndSnapNameContainingAndStatus(
        marathonId: Long,
        courseId: Long,
        name: String,
        status: RegistrationStatus,
        pageable: Pageable,
    ): Page<Registration>

    @EntityGraph(attributePaths = ["marathon", "course"])
    fun findByIdAndMarathon_IdAndStatus(
        registrationId: Long,
        marathonId: Long,
        status: RegistrationStatus,
    ): Registration?

    // 특정 상태의 참가자 이메일 목록 조회
    @Query(
        """
        SELECT DISTINCT r.user.email
        FROM Registration r
        WHERE r.course.marathon.id = :marathonId
          AND r.status = :status
        """
    )
    fun findParticipantEmailsByMarathonIdAndStatus(
        @Param("marathonId") marathonId: Long,
        @Param("status") status: RegistrationStatus,
    ): List<String>
}
