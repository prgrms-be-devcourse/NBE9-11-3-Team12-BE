package com.rungo.api.domain.registration.dto;

import com.rungo.api.domain.marathon.marathon.dto.PageRes;
import com.rungo.api.domain.registration.entity.Registration;
import com.rungo.api.domain.registration.enumtype.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "참가자 목록 조회 응답 DTO")
public record RegistrationParticipantListRes(

        @Schema(description = "참가자 목록")
        List<Item> content,

        @Schema(description = "페이지 정보")
        PageRes pageRes

) {

    public static RegistrationParticipantListRes from(Page<Registration> page) {
        return new RegistrationParticipantListRes(
                page.getContent().stream()
                        .map(Item::from)
                        .toList(),
                PageRes.from(page)
        );
    }

    // 참가자 목록 1건
    @Schema(description = "참가자 목록 항목 DTO")
    public record Item(
            @Schema(description = "접수 ID", example = "10")
            Long registrationId,

            @Schema(description = "참가자 이름", example = "홍길동")
            String name,

            @Schema(description = "참가자 전화번호", example = "010-1234-5678")
            String phoneNumber,

            @Schema(description = "티셔츠 사이즈", example = "L")
            String tSize,

            @Schema(description = "코스 ID", example = "2")
            Long courseId,

            @Schema(description = "코스 종류", example = "10KM")
            String courseType,

            @Schema(description = "접수 상태", example = "COMPLETED")
            RegistrationStatus status,

            @Schema(description = "접수 시각", example = "2020-02-02T02:02:02")
            LocalDateTime appliedAt

    ) {
        public static Item from(Registration registration) {
            return new Item(
                    registration.getId(),
                    registration.getSnapName(),
                    registration.getSnapPhoneNumber(),
                    registration.getTSize(),
                    registration.getCourse().getId(),
                    registration.getCourse().getCourseType(),
                    registration.getStatus(),
                    registration.getAppliedAt()
            );
        }
    }
}