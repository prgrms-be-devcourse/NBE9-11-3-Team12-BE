package com.rungo.api.domain.marathon.course.repository;

import com.rungo.api.domain.marathon.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByMarathon_IdOrderByIdAsc(Long marathonId);

    @Modifying
    @Query("""
            update Course c
            set c.currentCount = c.currentCount + 1
            where c.id = :courseId
              and c.currentCount < c.capacity
            """)
    int increaseCurrentCountIfNotFull(@Param("courseId") Long courseId);

    @Modifying
    @Query("""
            update Course c
            set c.currentCount = c.currentCount - 1
            where c.id = :courseId
              and c.currentCount > 0
            """)
    int decreaseCurrentCountIfPositive(@Param("courseId") Long courseId);
}
