package com.rungo.api.domain.marathon.marathon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

@Schema(description = "페이지 응답 DTO")
public record PageRes(

        @Schema(description = "현재 페이지 번호", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "전체 데이터 수", example = "93")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages

) {
    public static PageRes from(Page<?> page) {
        return new PageRes(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
