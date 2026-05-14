package com.rungo.api.domain.registration.entity;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import com.rungo.api.domain.registration.enumtype.RegistrationStatus;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "registrations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_registration_user_marathon", columnNames = {"user_id", "marathon_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marathon_id", nullable = false)
    private Marathon marathon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    @Column(name = "snap_name", nullable = false, length = 50)
    private String snapName;

    @Column(name = "snap_phone_number", nullable = false, length = 20)
    private String snapPhoneNumber;

    @Column(name = "snap_zip_code", nullable = false, length = 10)
    private String snapZipCode;

    @Column(name = "snap_address", nullable = false, length = 255)
    private String snapAddress;

    @Column(name = "snap_detail", length = 255)
    private String snapDetail;

    @Column(name = "t_size", nullable = false, length = 10)
    private String tSize;

    @Column(name = "applied_at", nullable = false)
    @CreatedDate
    private LocalDateTime appliedAt;

    @Column(name = "agreed_terms", nullable = false)
    private boolean agreedTerms;

    private Registration(
            Users user,
            Course course,
            Marathon marathon,
            RegistrationStatus status,
            String snapName,
            String snapPhoneNumber,
            String snapZipCode,
            String snapAddress,
            String snapDetail,
            String tSize,
            boolean agreedTerms
    ) {
        this.user = user;
        this.course = course;
        this.marathon = marathon;
        this.status = status;
        this.snapName = snapName;
        this.snapPhoneNumber = snapPhoneNumber;
        this.snapZipCode = snapZipCode;
        this.snapAddress = snapAddress;
        this.snapDetail = snapDetail;
        this.tSize = tSize;
        this.agreedTerms = agreedTerms;
    }

    public static Registration create(
            Users user,
            Course course,
            Marathon marathon,
            String snapZipCode,
            String snapAddress,
            String snapDetail,
            String tSize,
            boolean agreedTerms
    ) {
        return new Registration(
                user,
                course,
                marathon,
                RegistrationStatus.COMPLETED,
                user.getName(),
                user.getPhoneNumber(),
                snapZipCode,
                snapAddress,
                snapDetail,
                tSize,
                agreedTerms
        );
    }

}
