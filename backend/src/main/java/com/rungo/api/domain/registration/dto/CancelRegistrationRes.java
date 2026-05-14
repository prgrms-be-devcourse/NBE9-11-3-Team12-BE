package com.rungo.api.domain.registration.dto;

import com.rungo.api.domain.registration.entity.Registration;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "접수 취소 응답 DTO")
public record CancelRegistrationRes(

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

        @Schema(description = "접수 상태", example = "CANCELED")
        String status

) {
    public static CancelRegistrationRes from(Registration registration) {
        return new CancelRegistrationRes(
                registration.getId(),
                registration.getMarathon().getId(),
                registration.getMarathon().getTitle(),
                registration.getCourse().getId(),
                registration.getCourse().getCourseType(),
                registration.getStatus().name()
        );
    }
}
