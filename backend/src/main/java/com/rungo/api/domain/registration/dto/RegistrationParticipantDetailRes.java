package com.rungo.api.domain.registration.dto;

import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.enumtype.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "참가자 상세 조회 응답 DTO")
public record RegistrationParticipantDetailRes(

        @Schema(description = "접수 ID", example = "10")
        Long registrationId,

        @Schema(description = "마라톤 ID", example = "1")
        Long marathonId,

        @Schema(description = "마라톤 제목", example = "2026 서울 마라톤")
        String marathonTitle,

        @Schema(description = "코스 ID", example = "2")
        Long courseId,

        @Schema(description = "코스 종류", example = "10KM")
        String courseType,

        @Schema(description = "접수 상태", example = "COMPLETED")
        RegistrationStatus status,

        @Schema(description = "참가자 이름", example = "홍길동")
        String snapName,

        @Schema(description = "참가자 전화번호", example = "010-1234-5678")
        String snapPhoneNumber,

        @Schema(description = "우편번호", example = "06236")
        String snapZipCode,

        @Schema(description = "접수 주소", example = "서울특별시 강남구 테헤란로 212")
        String snapAddress,

        @Schema(description = "접수 상세 주소", example = "8층")
        String snapDetail,

        @Schema(description = "티셔츠 사이즈", example = "L")
        String tSize,

        @Schema(description = "약관 동의 여부", example = "true")
        boolean agreedTerms,

        @Schema(description = "접수 시각", example = "2020-02-02T02:02:02")
        LocalDateTime appliedAt

) {
    public static RegistrationParticipantDetailRes from(Registration registration) {
        return new RegistrationParticipantDetailRes(
                registration.getId(),
                registration.getMarathon().getId(),
                registration.getMarathon().getTitle(),
                registration.getCourse().getId(),
                registration.getCourse().getCourseType(),
                registration.getStatus(),

                registration.getSnapName(),
                registration.getSnapPhoneNumber(),
                registration.getSnapZipCode(),
                registration.getSnapAddress(),
                registration.getSnapDetail(),

                registration.getTSize(),
                registration.isAgreedTerms(),
                registration.getAppliedAt()
        );
    }
}