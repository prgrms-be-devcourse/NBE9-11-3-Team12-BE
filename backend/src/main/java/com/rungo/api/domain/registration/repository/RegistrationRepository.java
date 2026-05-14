package com.rungo.api.domain.registration.repository;

import com.rungo.api.domain.registration.entity.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    @EntityGraph(attributePaths = {"marathon", "course"})
    Page<Registration> findByUser_Id(Long userId, Pageable pageable);

//    @EntityGraph(attributePaths = {"marathon", "course"})
//    Page<Registration> findByUser_IdAndStatus(Long userId, RegistrationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "course"})
    List<Registration> findAllByMarathon_IdOrderByAppliedAtDesc(Long marathonId);

    @EntityGraph(attributePaths = {"course"})
    Page<Registration> findByMarathon_Id(Long marathonId, Pageable pageable);

    @EntityGraph(attributePaths = {"course"})
    Page<Registration> findByMarathon_IdAndCourse_Id(Long marathonId, Long courseId, Pageable pageable);

    @EntityGraph(attributePaths = {"course"})
    Page<Registration> findByMarathon_IdAndSnapNameContaining(Long marathonId, String name, Pageable pageable);

    @EntityGraph(attributePaths = {"course"})
    Page<Registration> findByMarathon_IdAndCourse_IdAndSnapNameContaining(Long marathonId, Long courseId, String name, Pageable pageable);

    @EntityGraph(attributePaths = {"marathon", "course"})
    Optional<Registration> findByIdAndMarathon_Id(Long registrationId, Long marathonId);

    @Query("""
    SELECT DISTINCT r.user.email
    FROM Registration r
    WHERE r.course.marathon.id = :marathonId
    """)
    List<String> findParticipantEmailsByMarathonId(@Param("marathonId") Long marathonId);
}
