package com.rungo.api.domain.marathon.marathon.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@Schema(description = "페이지 응답 DTO")
data class PageRes(

    @field:Schema(description = "현재 페이지 번호", example = "0")
    @JvmField
    val page: Int,

    @field:Schema(description = "페이지 크기", example = "20")
    @JvmField
    val size: Int,

    @field:Schema(description = "전체 데이터 수", example = "93")
    @JvmField
    val totalElements: Long,

    @field:Schema(description = "전체 페이지 수", example = "5")
    val totalPages: Int
) {

    companion object {
        fun from(page: Page<*>) = PageRes(
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }

}