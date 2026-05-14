package com.rungo.api.domain.registration.dto;

import com.rungo.api.domain.marathon.course.entity.Course;
import com.rungo.api.domain.marathon.marathon.entity.Marathon;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "주최자용 접수 요약 조회 응답 DTO")
public record RegistrationOverviewRes(

        @Schema(description = "마라톤 요약 정보")
        MarathonInfo marathon,

        @Schema(description = "코스별 접수 현황")
        List<CourseStatus> courseStatuses

) {

    public static RegistrationOverviewRes of(Marathon marathon, List<Course> courses) {
        List<CourseStatus> courseStatuses = courses.stream()
                .map(CourseStatus::from)
                .toList();

        int totalCurrentCount = courses.stream()
                .mapToInt(Course::getCurrentCount)
                .sum();

        int totalCapacity = courses.stream()
                .mapToInt(Course::getCapacity)
                .sum();

        return new RegistrationOverviewRes(
                MarathonInfo.of(marathon, totalCurrentCount, totalCapacity),
                courseStatuses
        );
    }

    // 마라톤 요약 정보
    @Schema(description = "마라톤 요약 정보 DTO")
    public record MarathonInfo(

            @Schema(description = "마라톤 ID", example = "1")
            Long marathonId,

            @Schema(description = "마라톤 제목", example = "2026 서울 마라톤")
            String marathonTitle,

            @Schema(description = "대회 일자", example = "2020-02-02")
            LocalDate eventDate,

            @Schema(description = "지역", example = "서울")
            String region,

            @Schema(description = "전체 접수 인원", example = "180")
            int totalCurrentCount,

            @Schema(description = "전체 정원", example = "500")
            int totalCapacity,

            @Schema(description = "전체 잔여 인원", example = "320")
            int totalRemainingCount,

            @Schema(description = "전체 모집률(%)", example = "36")
            int totalRecruitmentRate

    ) {
        public static MarathonInfo of(Marathon marathon, int totalCurrentCount, int totalCapacity) {
            int totalRemainingCount = Math.max(totalCapacity - totalCurrentCount, 0);

            return new MarathonInfo(
                    marathon.getId(),
                    marathon.getTitle(),
                    marathon.getEventDate(),
                    marathon.getRegion(),
                    totalCurrentCount,
                    totalCapacity,
                    totalRemainingCount,
                    calculateRate(totalCurrentCount, totalCapacity)
            );
        }
    }

    // 코스별 현황
    @Schema(description = "코스별 접수 현황 DTO")
    public record CourseStatus(

            @Schema(description = "코스 ID", example = "2")
            Long courseId,

            @Schema(description = "코스 타입", example = "10KM")
            String courseType,

            @Schema(description = "참가비", example = "50000")
            BigDecimal price,

            @Schema(description = "현재 접수 인원", example = "120")
            int currentCount,

            @Schema(description = "정원", example = "300")
            int capacity,

            @Schema(description = "잔여 인원", example = "180")
            int remainingCount,

            @Schema(description = "모집률(%)", example = "40")
            int recruitmentRate

    ) {
        public static CourseStatus from(Course course) {
            int currentCount = course.getCurrentCount();
            int capacity = course.getCapacity();
            int remainingCount = Math.max(capacity - currentCount, 0);

            return new CourseStatus(
                    course.getId(),
                    course.getCourseType(),
                    course.getPrice(),
                    currentCount,
                    capacity,
                    remainingCount,
                    calculateRate(currentCount, capacity)
            );
        }
    }

    private static int calculateRate(int currentCount, int capacity) {
        if (capacity <= 0) {
            return 0;
        }
        return (int) ((currentCount * 100.0) / capacity);
    }
}