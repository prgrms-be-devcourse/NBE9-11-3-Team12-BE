package com.rungo.api.domain.registration.dto

import com.rungo.api.domain.marathon.marathon.dto.PageRes
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import java.time.LocalDateTime

@Schema(description = "참가자 목록 조회 응답 DTO")
@JvmRecord
data class RegistrationParticipantListRes(
    @field:Schema(description = "참가자 목록")
    val content: List<Item>,

    @field:Schema(description = "페이지 정보")
    val pageRes: PageRes,
) {
    companion object {
        @JvmStatic
        fun from(page: Page<Registration>) = RegistrationParticipantListRes(
            content = page.content.map(Item::from),
            pageRes = PageRes.from(page),
        )
    }

    @Schema(description = "참가자 목록 항목 DTO")
    @JvmRecord
    data class Item(
        @field:Schema(description = "접수 ID", example = "10")
        val registrationId: Long,

        @field:Schema(description = "참가자 이름", example = "홍길동")
        val name: String,

        @field:Schema(description = "참가자 전화번호", example = "010-1234-5678")
        val phoneNumber: String,

        @field:Schema(description = "티셔츠 사이즈", example = "L")
        val tSize: String,

        @field:Schema(description = "코스 ID", example = "2")
        val courseId: Long,

        @field:Schema(description = "코스 종류", example = "10KM")
        val courseType: String,

        @field:Schema(description = "접수 상태", example = "COMPLETED")
        val status: RegistrationStatus,

        @field:Schema(description = "접수 시각", example = "2020-02-02T02:02:02")
        val appliedAt: LocalDateTime,
    ) {
        companion object {
            @JvmStatic
            fun from(registration: Registration) = Item (
                registrationId = registration.id,
                name = registration.snapName,
                phoneNumber = registration.snapPhoneNumber,
                tSize = registration.tSize,
                courseId = registration.course.id,
                courseType = registration.course.courseType,
                status = registration.status,
                appliedAt = registration.appliedAt,
            )
        }
    }
}