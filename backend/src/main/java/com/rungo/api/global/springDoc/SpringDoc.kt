package com.rungo.api.global.springDoc

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "RunGo API",
        version = "v1",
        description = "RunGo 백엔드 API 문서입니다.",
        contact = Contact(
            name = "RunGo GitHub",
            url = "https://github.com/prgrms-be-devcourse/NBE9-11-3-Team12-BE.git",
        ),
    ),
)
@SecurityScheme(
    name = "accessTokenCookie",
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = "accessToken",
    description = "로그인 성공 후 발급되는 accessToken 쿠키 기반 인증",
)
class SpringDoc {

    @Bean
    fun authApi(): GroupedOpenApi = groupedOpenApi(
        group = "01. Auth",
        path = "/api/v1/auth/**",
    )

    @Bean
    fun userApi(): GroupedOpenApi = groupedOpenApi(
        group = "02. Users",
        path = "/api/v1/users/**",
    )

    @Bean
    fun marathonApi(): GroupedOpenApi = groupedOpenApi(
        group = "03. Marathon",
        path = "/api/v1/marathons/**",
    )

    @Bean
    fun registrationApi(): GroupedOpenApi = groupedOpenApi(
        group = "04. Registration",
        path = "/api/v1/registrations/**",
    )

    @Bean
    fun organizerApi(): GroupedOpenApi = groupedOpenApi(
        group = "05. Organizer",
        path = "/api/v1/organizer/**",
    )

    @Bean
    fun adminApi(): GroupedOpenApi = groupedOpenApi(
        group = "06. Admin",
        path = "/api/v1/admin/**",
    )

    private fun groupedOpenApi(
        group: String,
        path: String,
    ): GroupedOpenApi = GroupedOpenApi.builder()
        .group(group)
        .pathsToMatch(path)
        .build()
}
