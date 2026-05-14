package com.rungo.api.domain.registration.entity;

import com.rungo.api.domain.registration.enumtype.RegistrationCancelReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "registration_cancel_histories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_registration_cancel_history_original_registration_id",
                        columnNames = {"original_registration_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RegistrationCancelHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_registration_id", nullable = false)
    private Long originalRegistrationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "marathon_id", nullable = false)
    private Long marathonId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

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

    @Column(name = "agreed_terms", nullable = false)
    private boolean agreedTerms;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", nullable = false)
    private RegistrationCancelReason cancelReason;

    @CreatedDate
    @Column(name = "canceled_at", nullable = false)
    private LocalDateTime canceledAt;

    private RegistrationCancelHistory(
            Long originalRegistrationId,
            Long userId,
            Long marathonId,
            Long courseId,
            String snapName,
            String snapPhoneNumber,
            String snapZipCode,
            String snapAddress,
            String snapDetail,
            String tSize,
            boolean agreedTerms,
            LocalDateTime appliedAt,
            RegistrationCancelReason cancelReason
    ) {
        this.originalRegistrationId = originalRegistrationId;
        this.userId = userId;
        this.marathonId = marathonId;
        this.courseId = courseId;
        this.snapName = snapName;
        this.snapPhoneNumber = snapPhoneNumber;
        this.snapZipCode = snapZipCode;
        this.snapAddress = snapAddress;
        this.snapDetail = snapDetail;
        this.tSize = tSize;
        this.agreedTerms = agreedTerms;
        this.appliedAt = appliedAt;
        this.cancelReason = cancelReason;
    }

    public static RegistrationCancelHistory create(Registration registration) {
        return create(registration, RegistrationCancelReason.USER_CANCELED);
    }

    public static RegistrationCancelHistory create(
            Registration registration,
            RegistrationCancelReason cancelReason
    ) {
        return new RegistrationCancelHistory(
                registration.getId(),
                registration.getUser().getId(),
                registration.getMarathon().getId(),
                registration.getCourse().getId(),
                registration.getSnapName(),
                registration.getSnapPhoneNumber(),
                registration.getSnapZipCode(),
                registration.getSnapAddress(),
                registration.getSnapDetail(),
                registration.getTSize(),
                registration.isAgreedTerms(),
                registration.getAppliedAt(),
                cancelReason
        );
    }
}
