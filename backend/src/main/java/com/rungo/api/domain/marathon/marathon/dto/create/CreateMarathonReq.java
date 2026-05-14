package com.rungo.api.domain.marathon.marathon.dto.create;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "마라톤 생성 요청 DTO")
public record CreateMarathonReq(

        @Schema(description = "대회명", example = "2026 서울 마라톤")
        @NotBlank(message = "대회명은 필수입니다.")
        String title,

        @Schema(description = "지역", example = "서울")
        @NotBlank(message = "지역은 필수입니다.")
        String region,

        @Schema(description = "상세주소", example = "서울특별시 송파구 올림픽로 424")
        @NotBlank(message = "상세주소는 필수입니다.")
        String detailedAddress,

        @Schema(description = "대회 일자", example = "2020-02-02")
        @NotNull(message = "대회 일자는 필수입니다.")
        LocalDate eventDate,

        @Schema(description = "포스터 이미지", type = "string", format = "binary")
        MultipartFile posterImage,

        @Schema(description = "접수 시작 일시", example = "2020-02-02T02:02:02")
        @NotNull(message = "접수 시작일시는 필수입니다.")
        LocalDateTime registrationStartAt,

        @Schema(description = "접수 종료 일시", example = "2020-02-02T02:02:02")
        @NotNull(message = "접수 종료일시는 필수입니다.")
        LocalDateTime registrationEndAt,

        @Schema(description = "코스 목록")
        @Valid
        @NotEmpty(message = "코스는 최소 1개 이상 등록해야 합니다.")
        List<CreateCourseItemReq> courses
) {
    public record CreateCourseItemReq(

            @Schema(description = "코스 타입", example = "10KM")
            @NotBlank(message = "코스 타입은 필수입니다.")
            String courseType,

            @Schema(description = "참가비", example = "50000")
            @NotNull(message = "참가비는 필수입니다.")
            @Min(value = 0, message = "참가비는 0 이상이어야 합니다.")
            BigDecimal price,

            @Schema(description = "정원", example = "300")
            @NotNull(message = "정원은 필수입니다.")
            @Min(value = 1, message = "정원은 1 이상이어야 합니다.")
            Integer capacity
    ) {
    }
}
