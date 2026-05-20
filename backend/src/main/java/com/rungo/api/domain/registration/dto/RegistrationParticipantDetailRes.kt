package com.rungo.api.domain.registration.dto

import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "참가자 상세 조회 응답 DTO")
data class RegistrationParticipantDetailRes(
    @field:Schema(description = "접수 ID", example = "10")
    val registrationId: Long,

    @field:Schema(description = "마라톤 ID", example = "1")
    val marathonId: Long,

    @field:Schema(description = "마라톤 제목", example = "2026 서울 마라톤")
    val marathonTitle: String,

    @field:Schema(description = "코스 ID", example = "2")
    val courseId: Long,

    @field:Schema(description = "코스 종류", example = "10KM")
    val courseType: String,

    @field:Schema(description = "접수 상태", example = "COMPLETED")
    val status: RegistrationStatus,

    @field:Schema(description = "참가자 이름", example = "홍길동")
    val snapName: String,

    @field:Schema(description = "참가자 전화번호", example = "010-1234-5678")
    val snapPhoneNumber: String,

    @field:Schema(description = "우편번호", example = "06236")
    val snapZipCode: String,

    @field:Schema(description = "접수 주소", example = "서울특별시 강남구 테헤란로 212")
    val snapAddress: String,

    @field:Schema(description = "접수 상세 주소", example = "8층")
    val snapDetail: String?,

    @field:Schema(description = "티셔츠 사이즈", example = "L")
    val tSize: String,

    @field:Schema(description = "약관 동의 여부", example = "true")
    val agreedTerms: Boolean,

    @field:Schema(description = "접수 시각", example = "2020-02-02T02:02:02")
    val appliedAt: LocalDateTime,
) {
    companion object {
        fun from(registration: Registration) = RegistrationParticipantDetailRes(
            registrationId = registration.id,
            marathonId = registration.marathon.id,
            marathonTitle = registration.marathon.title,
            courseId = registration.course.id,
            courseType = registration.course.courseType,
            status = registration.status,
            snapName = registration.snapName,
            snapPhoneNumber = registration.snapPhoneNumber,
            snapZipCode = registration.snapZipCode,
            snapAddress = registration.snapAddress,
            snapDetail = registration.snapDetail,
            tSize = registration.tSize,
            agreedTerms = registration.isAgreedTerms,
            appliedAt = registration.appliedAt,
        )
    }
}