package com.rungo.api.domain.marathon.marathon.dto.create

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "마라톤 생성 요청 DTO")
data class CreateMarathonReq(

    @field:Schema(description = "대회명", example = "2026 서울 마라톤")
    @field:NotBlank(message = "대회명은 필수입니다.")
    val title: String,

    @field:Schema(description = "지역", example = "서울")
    @field:NotBlank(message = "지역은 필수입니다.")
    val region: String,

    @field:Schema(description = "상세주소", example = "서울특별시 송파구 올림픽로 424")
    @field:NotBlank(message = "상세주소는 필수입니다.")
    val detailedAddress: String,

    @field:Schema(description = "대회 일자", example = "2020-02-02")
    val eventDate: LocalDate,

    @field:Schema(description = "포스터 이미지", type = "string", format = "binary")
    val posterImage: MultipartFile?,

    @field:Schema(description = "접수 시작 일시", example = "2020-02-02T02:02:02")
    val registrationStartAt: LocalDateTime,

    @field:Schema(description = "접수 종료 일시", example = "2020-02-02T02:02:02")
    val registrationEndAt: LocalDateTime,

    @field:Schema(description = "코스 목록")
    @field:Valid
    @field:NotEmpty(message = "코스는 최소 1개 이상 등록해야 합니다.")
    val courses: List<CreateCourseItemReq>
) {
    data class CreateCourseItemReq(

        @field:Schema(description = "코스 타입", example = "10KM")
        @field:NotBlank(message = "코스 타입은 필수입니다.")
        val courseType: String,

        @field:Schema(description = "참가비", example = "50000")
        @field:Min(value = 0, message = "참가비는 0 이상이어야 합니다.")
        @field:Digits(integer = 10, fraction = 0, message = "참가비는 정수 원 단위여야 합니다.")
        val price: BigDecimal,

        @field:Schema(description = "정원", example = "300")
        @field:Min(value = 1, message = "정원은 1 이상이어야 합니다.")
        val capacity: Int
    )
}