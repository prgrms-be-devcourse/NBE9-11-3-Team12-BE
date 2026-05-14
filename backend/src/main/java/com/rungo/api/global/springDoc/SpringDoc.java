package com.rungo.api.global.springDoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "RunGo API",
                version = "v1",
                description = "Rungo 백엔드 API 문서입니다.",
                contact = @Contact(
                        name = "Rungo GitHub",
                        url = "https://github.com/prgrms-be-devcourse/NBE9-11-2-Team12-BE.git"
                )
        )
)
@SecurityScheme(
        name = "accessTokenCookie",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "accessToken",
        description = "로그인 성공 후 발급되는 accessToken 쿠키 기반 인증"
)
public class SpringDoc {

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("01. Auth")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("02. Users")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi marathonApi() {
        return GroupedOpenApi.builder()
                .group("03. Marathon")
                .pathsToMatch("/api/v1/marathons/**")
                .build();
    }

    @Bean
    public GroupedOpenApi registrationApi() {
        return GroupedOpenApi.builder()
                .group("04. Registration")
                .pathsToMatch("/api/v1/registrations/**")
                .build();
    }

    @Bean
    public GroupedOpenApi organizerApi() {
        return GroupedOpenApi.builder()
                .group("05. Organizer")
                .pathsToMatch("/api/v1/organizer/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("06. Admin")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }
}
