package com.rungo.api.domain.registration.controller

import com.rungo.api.domain.registration.dto.RegistrationOverviewRes
import com.rungo.api.domain.registration.dto.RegistrationParticipantDetailRes
import com.rungo.api.domain.registration.dto.RegistrationParticipantListRes
import com.rungo.api.domain.registration.service.RegistrationOrganizerQueryService
import com.rungo.api.global.response.ApiResponse
import com.rungo.api.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Validated
@RestController
@RequestMapping("/api/v1/organizer/marathons/{id}/registrations")
@Tag(name = "Organizer Registration Query", description = "주최자용 접수 조회 API")
@SecurityRequirement(name = "accessTokenCookie")
class RegistrationOrganizerQueryController(
    private val registrationOrganizerQueryService: RegistrationOrganizerQueryService,
) {
    // 주최자 - 접수 요약 조회
    @GetMapping("/summary")
    @Operation(summary = "접수 요약 조회", description = "특정 마라톤의 접수 요약 및 코스별 현황을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "401", description = "인증 필요"),
            SwaggerResponse(responseCode = "403", description = "권한 없음"),
            SwaggerResponse(responseCode = "404", description = "마라톤 없음"),
        ],
    )
    fun getRegistrationOverview(
        @AuthenticationPrincipal organizer: SecurityUser,
        @PathVariable("id") marathonId: Long,
    ): ResponseEntity<ApiResponse<RegistrationOverviewRes>> {
        val result = registrationOrganizerQueryService.getRegistrationOverview(
            organizer.id,
            marathonId,
        )

        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    // 주최자 - 참가자 목록 조회
    @GetMapping
    @Operation(summary = "참가자 목록 조회", description = "코스 ID, 이름, 페이지 조건으로 참가자 목록을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "400", description = "페이지 파라미터 검증 실패"),
            SwaggerResponse(responseCode = "401", description = "인증 필요"),
            SwaggerResponse(responseCode = "403", description = "권한 없음"),
            SwaggerResponse(responseCode = "404", description = "마라톤 없음"),
        ],
    )
    fun getMarathonParticipants(
        @AuthenticationPrincipal organizer: SecurityUser,
        @PathVariable("id") marathonId: Long,
        @RequestParam(required = false) courseId: Long?,
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "0")
        @Min(value = 0, message = "page는 0 이상이어야 합니다.")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        size: Int,
    ): ResponseEntity<ApiResponse<RegistrationParticipantListRes>> {
        // 신청일 최신순, 같은 신청일이면 id 역순으로 고정
        val pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.desc("appliedAt"),
                Sort.Order.desc("id"),
            ),
        )

        val result = registrationOrganizerQueryService.getMarathonParticipants(
            organizer.id,
            marathonId,
            courseId,
            name,
            pageable,
        )

        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    // 주최자 - 참가자 상세 조회
    @GetMapping("/{registrationId}")
    @Operation(summary = "참가자 상세 조회", description = "특정 참가자의 접수 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerResponse(responseCode = "200", description = "조회 성공"),
            SwaggerResponse(responseCode = "401", description = "인증 필요"),
            SwaggerResponse(responseCode = "403", description = "권한 없음"),
            SwaggerResponse(responseCode = "404", description = "접수 또는 마라톤 없음"),
        ],
    )
    fun getMarathonParticipantDetail(
        @AuthenticationPrincipal organizer: SecurityUser,
        @PathVariable("id") marathonId: Long,
        @PathVariable registrationId: Long,
    ): ResponseEntity<ApiResponse<RegistrationParticipantDetailRes>> {
        val result = registrationOrganizerQueryService.getMarathonParticipantDetail(
            organizer.id,
            marathonId,
            registrationId,
        )

        return ResponseEntity.ok(ApiResponse.ok(result))
    }
}