package com.rungo.api.domain.marathon.course.entity

import com.rungo.api.domain.marathon.course.status.CourseStatus
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
class Course protected constructor() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marathon_id", nullable = false)
    lateinit var marathon: Marathon
        protected set

    @Column(nullable = false)
    lateinit var courseType: String
        protected set

    @Column(nullable = false, precision = 10, scale = 2)
    lateinit var price: BigDecimal
        protected set

    @Column(nullable = false)
    var capacity: Int = 0
        protected set

    @Column(nullable = false)
    var currentCount: Int = 0
        protected set

    fun assignMarathon(marathon: Marathon) {
        this.marathon = marathon
    }

    fun getStatus(): CourseStatus {
        return if (currentCount >= capacity) {
            CourseStatus.FULL
        } else {
            CourseStatus.AVAILABLE
        }
    }

    fun isFull(): Boolean {
        return currentCount >= capacity
    }

    fun resetCurrentCount() {
        currentCount = 0
    }

    fun getRemainingCount(): Int {
        return capacity - currentCount
    }

    fun updateCourseInfo(
        courseType: String?,
        price: BigDecimal?,
        capacity: Int?
    ) {
        if (courseType != null) this.courseType = courseType
        if (price != null) this.price = price
        if (capacity != null) this.capacity = capacity
    }

    companion object {

        @JvmStatic
        fun create(
            courseType: String,
            price: BigDecimal,
            capacity: Int,
            currentCount: Int
        ): Course {

            return Course().apply {
                this.courseType = courseType
                this.price = price
                this.capacity = capacity
                this.currentCount = currentCount
            }
        }
    }
}