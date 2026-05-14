package com.rungo.api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 DTO")
public record LoginReq(

        @Schema(description = "이메일", example = "test@example.com")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @Schema(description = "비밀번호", example = "Abcd1234!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password

) {}