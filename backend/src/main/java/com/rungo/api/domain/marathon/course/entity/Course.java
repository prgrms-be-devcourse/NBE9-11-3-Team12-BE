package com.rungo.api.domain.marathon.course.entity;

import com.rungo.api.domain.marathon.course.status.CourseStatus;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marathon_id", nullable = false)
    private Marathon marathon;

    @Column(nullable = false)
    private String courseType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Integer currentCount;

    public Course(
            String courseType,
            BigDecimal price,
            Integer capacity,
            Integer currentCount
    ) {
        this.courseType = courseType;
        this.price = price;
        this.capacity = capacity;
        this.currentCount = currentCount;
    }

    public CourseStatus getStatus() {
        return currentCount >= capacity ? CourseStatus.FULL : CourseStatus.AVAILABLE;
    }
    public boolean isFull() {
        return currentCount >= capacity;
    }

    public void resetCurrentCount() {
        this.currentCount = 0;
    }

    public Integer getRemainingCount(){
        return this.capacity - this.currentCount;
    }

    public void updateCourseInfo(
            String courseType,
            BigDecimal price,
            Integer capacity
    ) {
        if (courseType != null) this.courseType = courseType;
        if (price != null) this.price = price;
        if (capacity != null) this.capacity = capacity;
    }
}
