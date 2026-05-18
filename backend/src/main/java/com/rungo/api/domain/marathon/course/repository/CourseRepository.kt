package com.rungo.api.domain.marathon.course.repository

import com.rungo.api.domain.marathon.course.entity.Course
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CourseRepository : JpaRepository<Course, Long> {

    fun findAllByMarathon_IdOrderByIdAsc(
        marathonId: Long
    ): List<Course>

    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query(
        """
        update Course c
        set c.currentCount = c.currentCount + 1
        where c.id = :courseId
          and c.currentCount < c.capacity
        """
    )
    fun increaseCurrentCountIfNotFull(
        @Param("courseId")
        courseId: Long
    ): Int

    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query(
        """
        update Course c
        set c.currentCount = c.currentCount - 1
        where c.id = :courseId
          and c.currentCount > 0
        """
    )
    fun decreaseCurrentCountIfPositive(
        @Param("courseId")
        courseId: Long
    ): Int
}