package com.rungo.api.domain.marathon.marathon.dto.update;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "마라톤 수정 요청 DTO")
public record UpdateMarathonReq(

        @Schema(description = "대회명", example = "2026 서울 국제 마라톤")
        String title,

        @Schema(description = "지역", example = "서울")
        String region,

        @Schema(description = "상세주소", example = "서울특별시 송파구 올림픽로 424")
        String detailedAddress,

        @Schema(description = "대회 일자", example = "2020-02-02")
        LocalDate eventDate,

        @Schema(description = "포스터 이미지", type = "string", format = "binary")
        MultipartFile posterImage,

        @Schema(description = "접수 시작 일시", example = "2020-02-02T02:02:02")
        LocalDateTime registrationStartAt,

        @Schema(description = "접수 종료 일시", example = "2020-02-02T02:02:02")
        LocalDateTime registrationEndAt,

        @Schema(description = "수정할 코스 목록")
        @Valid
        List<UpdateCourseItemReq> courses

) {
    @Schema(description = "마라톤 코스 수정용 요청 DTO")
    public record UpdateCourseItemReq(

            @Schema(description = "코스 ID", example = "1")
            @NotNull(message = "코스 아이디는 필수입니다.")
            Long id,

            @Schema(description = "코스 타입", example = "10KM")
            String courseType,

            @Schema(description = "참가비", example = "55000")
            @Min(value = 0, message = "참가비는 0 이상이어야 합니다.")
            BigDecimal price,

            @Schema(description = "정원", example = "350")
            @Min(value = 1, message = "정원은 1 이상이어야 합니다.")
            Integer capacity

    ) {
    }
}
