package com.rungo.api.domain.registration.dto

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.marathon.dto.PageRes
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "내 접수 목록 조회 응답 DTO")
@JvmRecord
data class MyRegistrationRes(

    @field:Schema(description = "접수 목록")
    val content: List<Item>,

    @field:Schema(description = "페이지 정보")
    val pageRes: PageRes,

    ) {

    companion object {

        // 정상 접수 목록 DTO 변환
        @JvmStatic
        fun fromActive(page: Page<Registration>) = MyRegistrationRes(
            content = page.content.map(Item::fromActive),
            pageRes = PageRes.from(page)
        )

        // 접수 취소 목록 DTO 변환
        @JvmStatic
        fun fromCanceled(

            page: Page<RegistrationCancelHistory>,
            marathonMap: Map<Long, Marathon>,
            courseMap: Map<Long, Course>

        ) = MyRegistrationRes(

            content = page.content.map { history ->
                Item.fromCanceled(
                    history = history,
                    marathon = marathonMap.getValue(history.marathonId),
                    course = courseMap.getValue(history.courseId),
                )
            },

            pageRes = PageRes.from(page)
        )
    }

    // ACTIVE, CANCELED 공통 구조 => 일부 필드는 null
    @Schema(description = "내 접수 목록 항목 DTO")
    @JvmRecord
    data class Item(
        @field:Schema(description = "정상 접수 조회 시 현재 접수 ID / 취소 이력 조회 시 취소 이력 ID", example = "100")
        val registrationId: Long,

        @field:Schema(description = "취소 이력 조회 시 원본 접수 ID", example = "200")
        val historyId: Long?,

        @field:Schema(description = "마라톤 ID", example = "1")
        val marathonId: Long,

        @field:Schema(description = "마라톤 제목", example = "2026 서울 마라톤")
        val marathonTitle: String,

        @field:Schema(description = "코스 ID", example = "10")
        val courseId: Long,

        @field:Schema(description = "코스 종류", example = "10KM")
        val courseType: String,

        @field:Schema(description = "접수 상태", example = "ACTIVE")
        val status: String,

        @field:Schema(description = "결제 금액", example = "50000")
        val price: BigDecimal,

        @field:Schema(description = "대회 날짜", example = "2020-02-02")
        val eventDate: LocalDate,

        @field:Schema(description = "접수 이름", example = "홍길동")
        val snapName: String,

        @field:Schema(description = "접수 전화번호", example = "010-1234-5678")
        val snapPhoneNumber: String,

        @field:Schema(description = "접수 우편번호", example = "12345")
        val snapZipCode: String,

        @field:Schema(description = "접수 주소", example = "서울시 강남구 ...")
        val snapAddress: String,

        @field:Schema(description = "접수 상세주소", example = "101동 202호")
        val snapDetail: String?,

        @field:Schema(description = "티셔츠 사이즈", example = "L")
        val tSize: String,

        @field:Schema(description = "약관 동의 여부", example = "true")
        val agreedTerms: Boolean,

        @field:Schema(description = "접수 시각", example = "2020-02-02T02:02:02")
        val appliedAt: LocalDateTime,

        @field:Schema(description = "취소 시각", example = "2020-02-02T02:02:02")
        val canceledAt: LocalDateTime?,
    ) {

        // 정상 접수 Entity를 응답용 item 변환 (취소 전용 필드는 null : originalRegistrationId, canceledAt)
        companion object {

            @JvmStatic
            fun fromActive(registration: Registration) = Item(

                registrationId = registration.id,
                historyId = null,
                marathonId = registration.marathon.id,
                marathonTitle = registration.marathon.title,
                courseId = registration.course.id,
                courseType = registration.course.courseType,
                status = "ACTIVE",
                price = registration.course.price,
                eventDate = registration.marathon.eventDate,

                snapName = registration.snapName,
                snapPhoneNumber = registration.snapPhoneNumber,
                snapZipCode = registration.snapZipCode,
                snapAddress = registration.snapAddress,
                snapDetail = registration.snapDetail,
                tSize = registration.tSize,
                agreedTerms = registration.isAgreedTerms,

                appliedAt = registration.appliedAt,
                canceledAt = null,

            )

            // 취소 접수 Entity를 응답용 item 변환
            @JvmStatic
            fun fromCanceled(

                history: RegistrationCancelHistory,
                marathon: Marathon,
                course: Course,

            ) = Item(

                registrationId = history.id,
                historyId = history.originalRegistrationId,
                marathonId = history.marathonId,
                marathonTitle = marathon.title,
                courseId = history.courseId,
                courseType = course.courseType,
                status = "CANCELED",
                price = course.price,
                eventDate = marathon.eventDate,

                snapName = history.snapName,
                snapPhoneNumber = history.snapPhoneNumber,
                snapZipCode = history.snapZipCode,
                snapAddress = history.snapAddress,
                snapDetail = history.snapDetail,
                tSize = history.tSize,
                agreedTerms = history.isAgreedTerms,

                appliedAt = history.appliedAt,
                canceledAt = history.canceledAt,
            )
        }
    }
}