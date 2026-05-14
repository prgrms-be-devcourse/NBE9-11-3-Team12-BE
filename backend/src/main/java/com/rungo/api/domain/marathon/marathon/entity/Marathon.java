package com.rungo.api.domain.marathon.marathon.entity;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus;
import com.rungo.api.domain.marathon.marathon.enumtype.RecruitmentStatus;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "Marathon",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_marathon_organizerId_title_eventDate",
                        columnNames = {"organizer_id", "title", "event_date"})
        }

)
public class Marathon {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Users organizer;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(name = "detailed_address", nullable = false,length = 100)
    private String detailedAddress;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(length = 500)
    private String posterImageUrl;

    @Column(name = "created_at", nullable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "registration_start_at", nullable = false)
    private LocalDateTime registrationStartAt;

    @Column(name = "registration_end_at", nullable = false)
    private LocalDateTime registrationEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarathonStatus status;

    @OneToMany(mappedBy = "marathon", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Course> courses = new ArrayList<>();

    public Marathon(
            Users organizer,
            String title,
            String region,
            String detailedAddress,
            LocalDate eventDate,
            String posterImageUrl,
            LocalDateTime registrationStartAt,
            LocalDateTime registrationEndAt,
            MarathonStatus status
    ) {
        this.organizer = organizer;
        this.title = title;
        this.region = region;
        this.detailedAddress = detailedAddress;
        this.eventDate = eventDate;
        this.posterImageUrl = posterImageUrl;
        this.registrationStartAt = registrationStartAt;
        this.registrationEndAt = registrationEndAt;
        this.status = status;
    }

    public boolean isOpen() {
        return this.status == MarathonStatus.OPEN;
    }

    public boolean isCanceled() {
        return (this.status == MarathonStatus.CANCELING || this.status == MarathonStatus.CANCELED);
    }

    public void addCourse(Course course) {
        this.courses.add(course);
        course.setMarathon(this);
    }

    public void cancel() {
        if (this.status == MarathonStatus.CANCELED || this.status == MarathonStatus.CANCELING) {
            throw new CustomException(ErrorCode.MARATHON_ALREADY_CANCELED);
        }
        this.status = MarathonStatus.CANCELED;
    }

    public static Marathon create(

            Users organizer,
            String title,
            String region,
            String detailedAddress,
            LocalDate eventDate,
            String posterImageUrl,
            LocalDateTime registrationStartAt,
            LocalDateTime registrationEndAt

    ) {

        return new Marathon(

                organizer,
                title,
                region,
                detailedAddress,
                eventDate,
                posterImageUrl,
                registrationStartAt,
                registrationEndAt,
                MarathonStatus.OPEN

        );

    }

    public void updateMarathonInfo(
            String title,
            String region,
            String detailedAddress,
            LocalDate eventDate,
            String posterImageUrl,
            LocalDateTime registrationStartAt,
            LocalDateTime registrationEndAt
    ) {
        if (title != null) this.title = title;
        if (region != null) this.region = region;
        if (detailedAddress != null) this.detailedAddress = detailedAddress;
        if (eventDate != null) this.eventDate = eventDate;
        if (posterImageUrl != null) this.posterImageUrl = posterImageUrl;
        if (registrationStartAt != null) this.registrationStartAt = registrationStartAt;
        if (registrationEndAt != null) this.registrationEndAt = registrationEndAt;
    }
    public void open(){
        this.status = MarathonStatus.OPEN;
    }
    public boolean isAllCoursesFull() {
        return this.courses.stream().allMatch(Course::isFull);
    }

    public RecruitmentStatus getRecruitmentStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (this.isCanceled()) {
            return RecruitmentStatus.CANCELED;
        }
        if (now.isBefore(this.registrationStartAt)) {
            return RecruitmentStatus.TEMP;
        }
        if (now.isAfter(this.registrationEndAt)) {
            return RecruitmentStatus.CLOSED;
        }
        if (isAllCoursesFull()) {
            return RecruitmentStatus.FULL;
        }
        return RecruitmentStatus.OPEN;

    }
}

