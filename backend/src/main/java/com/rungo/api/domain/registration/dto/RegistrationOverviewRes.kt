package com.rungo.api.domain.registration.dto

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "주최자용 접수 요약 조회 응답 DTO")
data class RegistrationOverviewRes(
    @field:Schema(description = "마라톤 요약 정보")
    val marathon: MarathonInfo,

    @field:Schema(description = "코스별 접수 현황")
    val courseStatuses: List<CourseStatus>,
) {
    companion object {
        fun of(marathon: Marathon, courses: List<Course>): RegistrationOverviewRes {
            val courseStatuses = courses.map(CourseStatus::from)
            val totalCurrentCount = courses.sumOf { it.currentCount }
            val totalCapacity = courses.sumOf { it.capacity }

            return RegistrationOverviewRes(
                marathon = MarathonInfo.of(
                    marathon = marathon,
                    totalCurrentCount = totalCurrentCount,
                    totalCapacity = totalCapacity,
                ),
                courseStatuses = courseStatuses,
            )
        }

        private fun calculateRate(currentCount: Int, capacity: Int): Int {
            if (capacity <= 0) return 0
            return ((currentCount * 100.0) / capacity).toInt()
        }
    }

    @Schema(description = "마라톤 요약 정보 DTO")
        data class MarathonInfo(
        @field:Schema(description = "마라톤 ID", example = "1")
        val marathonId: Long,

        @field:Schema(description = "마라톤 제목", example = "2026 서울 마라톤")
        val marathonTitle: String,

        @field:Schema(description = "대회 일자", example = "2020-02-02")
        val eventDate: LocalDate,

        @field:Schema(description = "지역", example = "서울")
        val region: String,

        @field:Schema(description = "전체 선점 인원. 결제 완료 접수와 결제 대기 접수를 포함", example = "180")
        val totalCurrentCount: Int,

        @field:Schema(description = "전체 정원", example = "500")
        val totalCapacity: Int,

        @field:Schema(description = "전체 잔여 인원", example = "320")
        val totalRemainingCount: Int,

        @field:Schema(description = "전체 모집률(%)", example = "36")
        val totalRecruitmentRate: Int,
    ) {
        companion object {
                fun of(
                marathon: Marathon,
                totalCurrentCount: Int,
                totalCapacity: Int,
            ): MarathonInfo {
                val totalRemainingCount = (totalCapacity - totalCurrentCount).coerceAtLeast(0)

                return MarathonInfo(
                    marathonId = marathon.id,
                    marathonTitle = marathon.title,
                    eventDate = marathon.eventDate,
                    region = marathon.region,
                    totalCurrentCount = totalCurrentCount,
                    totalCapacity = totalCapacity,
                    totalRemainingCount = totalRemainingCount,
                    totalRecruitmentRate = calculateRate(totalCurrentCount, totalCapacity),
                )
            }
        }
    }

    @Schema(description = "코스별 접수 현황 DTO")
        data class CourseStatus(
        @field:Schema(description = "코스 ID", example = "2")
        val courseId: Long,

        @field:Schema(description = "코스 타입", example = "10KM")
        val courseType: String,

        @field:Schema(description = "참가비", example = "50000")
        val price: BigDecimal,

        @field:Schema(description = "코스별 선점 인원. 결제 완료 접수와 결제 대기 접수를 포함", example = "120")
        val currentCount: Int,

        @field:Schema(description = "정원", example = "300")
        val capacity: Int,

        @field:Schema(description = "잔여 인원", example = "180")
        val remainingCount: Int,

        @field:Schema(description = "모집률(%)", example = "40")
        val recruitmentRate: Int,
    ) {
        companion object {
                fun from(course: Course): CourseStatus {
                val currentCount = course.currentCount
                val capacity = course.capacity
                val remainingCount = (capacity - currentCount).coerceAtLeast(0)

                return CourseStatus(
                    courseId = course.id,
                    courseType = course.courseType,
                    price = course.price,
                    currentCount = currentCount,
                    capacity = capacity,
                    remainingCount = remainingCount,
                    recruitmentRate = calculateRate(currentCount, capacity),
                )
            }
        }
    }
}
