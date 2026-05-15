package com.rungo.api.domain.marathon.marathon.controller

import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonReq
import com.rungo.api.domain.marathon.marathon.dto.create.CreateMarathonRes
import com.rungo.api.domain.marathon.marathon.dto.delete.CancelMarathonRes
import com.rungo.api.domain.marathon.marathon.dto.read.MarathonDetailRes
import com.rungo.api.domain.marathon.marathon.dto.read.MarathonListRes
import com.rungo.api.domain.marathon.marathon.dto.read.ReadMyMarathonRes
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonReq
import com.rungo.api.domain.marathon.marathon.dto.update.UpdateMarathonRes
import com.rungo.api.domain.marathon.marathon.service.MarathonService
import com.rungo.api.global.response.ApiResponse
import com.rungo.api.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse

@RestController
@RequestMapping("/api/v1/marathons")
@Tag(name = "Marathon", description = "마라톤 대회 관련 API")
class MarathonController(
    private val marathonService: MarathonService,
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @SecurityRequirement(name = "accessTokenCookie")
    @Operation(summary = "마라톤 생성", description = "주최자 또는 관리자가 마라톤 대회를 생성합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "201", description = "마라톤 생성 성공"),
        SwaggerResponse(responseCode = "400", description = "입력값 검증 실패"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "403", description = "권한 없음")
    )
    fun createMarathon(
        @AuthenticationPrincipal user: SecurityUser,
        @Valid @ModelAttribute req: CreateMarathonReq
    ): ResponseEntity<ApiResponse<CreateMarathonRes>> {

        val res = marathonService.createMarathon(user.id, req)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("마라톤 대회 생성 성공", res))
    }

    @GetMapping
    @Operation(summary = "마라톤 목록 조회", description = "페이징 기반으로 마라톤 목록을 조회합니다.")
    @ApiResponses(SwaggerResponse(responseCode = "200", description = "목록 조회 성공"))
    fun getMarathonList(pageable: Pageable): ResponseEntity<ApiResponse<MarathonListRes>> =
        ResponseEntity.ok(
            ApiResponse.ok(marathonService.getMarathons(pageable))
        )


    @GetMapping("/{id}")
    @Operation(summary = "마라톤 상세 조회", description = "마라톤 ID로 상세 정보를 조회합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "상세 조회 성공"),
        SwaggerResponse(responseCode = "404", description = "마라톤 없음")
    )
    fun getMarathonDetail(@PathVariable id: Long): ResponseEntity<ApiResponse<MarathonDetailRes>> =
         ResponseEntity.ok(
            ApiResponse.ok(marathonService.getMarathonDetail(id))
        )


    @GetMapping("/me")
    @SecurityRequirement(name = "accessTokenCookie")
    @Operation(summary = "내 마라톤 조회", description = "현재 로그인한 사용자가 생성한 마라톤 목록을 조회합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "조회 성공"),
        SwaggerResponse(responseCode = "401", description = "인증 필요")
    )
    fun getMyMarathons(
        @AuthenticationPrincipal user: SecurityUser
    ): ResponseEntity<ApiResponse<List<ReadMyMarathonRes>>> {

        val res = marathonService.getMyMarathons(user.id)
        return ResponseEntity.ok(ApiResponse.ok(res))
    }

    @PatchMapping("/{id}/cancel")
    @SecurityRequirement(name = "accessTokenCookie")
    @Operation(summary = "마라톤 취소", description = "마라톤 대회를 취소 상태로 변경합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "200", description = "취소 성공"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "403", description = "권한 없음"),
        SwaggerResponse(responseCode = "404", description = "마라톤 없음")
    )
    fun cancelMarathon(
        @AuthenticationPrincipal user: SecurityUser,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<CancelMarathonRes>> {

        val res = marathonService.cancelMarathon(user.id, id)
        return ResponseEntity.ok(ApiResponse.ok(res))
    }

    @PatchMapping("/{marathonId}")
    @SecurityRequirement(name = "accessTokenCookie")
    @Operation(summary = "마라톤 수정", description = "기존 마라톤 정보를 수정합니다.")
    @ApiResponses(
        SwaggerResponse(responseCode = "201", description = "수정 성공"),
        SwaggerResponse(responseCode = "400", description = "입력값 검증 실패"),
        SwaggerResponse(responseCode = "401", description = "인증 필요"),
        SwaggerResponse(responseCode = "403", description = "권한 없음"),
        SwaggerResponse(responseCode = "404", description = "마라톤 없음")
    )
    fun update(
        @AuthenticationPrincipal user: SecurityUser,
        @PathVariable marathonId: Long,
        @Valid @ModelAttribute req: UpdateMarathonReq
    ): ResponseEntity<ApiResponse<UpdateMarathonRes>> {
        val res = marathonService.updateMarathon(user.id, marathonId, req)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created("마라톤 대회 수정 성공", res))
    }
}
