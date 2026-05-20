package com.rungo.api.domain.marathon.marathon.dto.read

import com.rungo.api.domain.marathon.marathon.dto.PageRes
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import com.rungo.api.domain.marathon.marathon.enumtype.RecruitmentStatus
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "마라톤 목록 조회 응답 DTO")
data class MarathonListRes(

    @field:Schema(description = "마라톤 목록")
    val content: List<Item>?,

    @field:Schema(description = "페이지 정보")
    val pageRes: PageRes?
) {

    companion object {
        fun from(page: Page<Marathon>): MarathonListRes {
            return MarathonListRes(
                content = page.content
                    .map { Item.from(it) },

                pageRes = PageRes.from(page)
            )
        }

        private fun calculateStatus(
            marathon: Marathon,
            totalCapacity: Int,
            totalCurrentCount: Int
        ): RecruitmentStatus {

            val now = LocalDateTime.now()

            if (marathon.isCanceled()) {
                return RecruitmentStatus.CANCELED
            }

            if (now.isBefore(marathon.registrationStartAt)) {
                return RecruitmentStatus.TEMP
            }

            if (now.isAfter(marathon.registrationEndAt)) {
                return RecruitmentStatus.CLOSED
            }

            if (totalCurrentCount >= totalCapacity) {
                return RecruitmentStatus.FULL
            }

            return RecruitmentStatus.OPEN
        }
    }

    @Schema(description = "마라톤 목록 항목 DTO")
    data class Item(

        @field:Schema(description = "마라톤 ID", example = "1")
        val id: Long,

        @field:Schema(description = "대회명", example = "2026 서울 마라톤")
        val title: String,

        @field:Schema(description = "지역", example = "서울")
        val region: String,

        @field:Schema(description = "상세주소", example = "서울특별시 송파구 올림픽로 424")
        val detailedAddress: String?,

        @field:Schema(description = "대회 일자", example = "2020-02-02")
        val eventDate: LocalDate,

        @field:Schema(description = "포스터 이미지 URL", example = "https://example.com/poster.png")
        val posterImageUrl: String?,

        @field:Schema(description = "접수 시작 일시", example = "2020-02-02T02:02:02")
        val registrationStartAt: LocalDateTime,

        @field:Schema(description = "접수 종료 일시", example = "2020-02-02T02:02:02")
        val registrationEndAt: LocalDateTime,

        @field:Schema(description = "마라톤 상태", example = "OPEN")
        val status: MarathonStatus,

        @field:Schema(description = "전체 정원", example = "500")
        val totalCapacity: Int,

        @field:Schema(description = "전체 접수 인원", example = "180")
        val totalCurrentCount: Int,

        @field:Schema(description = "모집 상태", example = "OPEN")
        val recruitmentStatus: RecruitmentStatus
    ) {

        companion object {
            fun from(marathon: Marathon): Item {

                val totalCapacity = marathon.courses
                    .sumOf { it.capacity }

                val totalCurrentCount = marathon.courses
                    .sumOf { it.currentCount }

                val recruitmentStatus = calculateStatus(
                    marathon,
                    totalCapacity,
                    totalCurrentCount
                )

                return Item(
                    id = marathon.id,
                    title = marathon.title,
                    region = marathon.region,
                    detailedAddress = marathon.detailedAddress,
                    eventDate = marathon.eventDate,
                    posterImageUrl = marathon.posterImageUrl,
                    registrationStartAt = marathon.registrationStartAt,
                    registrationEndAt = marathon.registrationEndAt,
                    status = marathon.status,
                    totalCapacity = totalCapacity,
                    totalCurrentCount = totalCurrentCount,
                    recruitmentStatus = recruitmentStatus
                )
            }
        }
    }
}