package com.rungo.api.domain.registration.controller

import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.dto.CreateRegistrationRes
import com.rungo.api.domain.registration.dto.MyRegistrationRes
import com.rungo.api.domain.registration.enumtype.MyRegistrationStatusFilter
import com.rungo.api.domain.registration.queue.service.RegistrationQueueService
import com.rungo.api.domain.registration.service.RegistrationService
import com.rungo.api.global.response.ApiResponse
import com.rungo.api.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@Validated
@RestController
@RequestMapping("/api/v1/registrations")
@Tag(name = "Registration", description = "사용자 접수 API")
@SecurityRequirement(name = "accessTokenCookie")
class RegistrationController(
    private val registrationQueueService: RegistrationQueueService,
    private val registrationService: RegistrationService,
) {
    @PostMapping
    @Operation(summary = "접수 생성", description = "현재 로그인한 사용자가 특정 마라톤 코스에 접수합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "201", description = "접수 성공"),
        SwaggerResponse(responseCode = "400", description = "입력값 오류 또는 접수 불가 상태"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "404", description = "마라톤 또는 코스 없음"),
        SwaggerResponse(responseCode = "409", description = "이미 접수함"),
    )
    fun create(
        @AuthenticationPrincipal user: SecurityUser,
        @Valid @RequestBody request: CreateRegistrationReq,
    ): ResponseEntity<ApiResponse<CreateRegistrationRes>> {
        val createRegistrationRes = registrationQueueService.create(user.id, request)

        // 무료/유료 응답 메세지 분기
        val message = if (createRegistrationRes.paymentStatus == null) {
            "접수가 완료되었습니다."
        } else {
            "접수 신청이 완료되었습니다. 결제를 진행해주세요."
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(message, createRegistrationRes))
    }

    @DeleteMapping("/{registrationId}")
    @Operation(summary = "접수 취소", description = "내 접수를 취소합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "접수 취소 성공"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "404", description = "접수 내역 없음"),
    )
    fun cancel(
        @AuthenticationPrincipal user: SecurityUser,
        @PathVariable registrationId: Long,
    ): ResponseEntity<ApiResponse<Void?>> {
        registrationService.cancel(user.id, registrationId)

        return ResponseEntity.ok(ApiResponse.okMessage("접수가 취소되었습니다. 결제 완료 건은 환불 요청 상태로 전환되며 순차 처리됩니다."))
    }

    @GetMapping("/me")
    @Operation(summary = "내 접수 목록 조회", description = "상태 필터와 페이징 조건으로 내 접수 목록을 조회합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "조회 성공"),
        SwaggerResponse(responseCode = "400", description = "페이지 파라미터 검증 실패"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
    )
    fun getMyRegistrations(
        @AuthenticationPrincipal user: SecurityUser,
        @RequestParam(defaultValue = "ACTIVE") status: MyRegistrationStatusFilter,
        @RequestParam(defaultValue = "0")
        @Min(value = 0, message = "page는 0 이상이어야 합니다.")
        page: Int,
        @RequestParam(defaultValue = "20")
        @Min(value = 1, message = "size는 1 이상이어야 합니다.")
        @Max(value = 100, message = "size는 100 이하여야 합니다.")
        size: Int,
    ): ResponseEntity<ApiResponse<MyRegistrationRes>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result = registrationService.getMyRegistrations(user.id, status, pageable)

        return ResponseEntity.ok(ApiResponse.ok(result))
    }
}
