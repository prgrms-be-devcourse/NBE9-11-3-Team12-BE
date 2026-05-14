package com.rungo.api.domain.users.dto;

import com.rungo.api.domain.users.enumtype.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CompleteProfileReq(
        @NotBlank String name,

        @NotBlank
        @Pattern(
                regexp = "^010-\\d{4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)"
        )
        String phoneNumber,

        @NotNull Gender gender,

        @NotNull
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birth
) {
}