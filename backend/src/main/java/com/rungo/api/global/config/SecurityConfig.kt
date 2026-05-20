package com.rungo.api.global.config

import com.rungo.api.domain.auth.handler.OAuth2AuthenticationFailureHandler
import com.rungo.api.domain.auth.handler.OAuth2AuthenticationSuccessHandler
import com.rungo.api.domain.auth.service.CustomOAuth2UserService
import com.rungo.api.global.security.CustomAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customAuthenticationFilter: CustomAuthenticationFilter,
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler,

    @param:Value("\${app.frontend.url:http://localhost:3000}")
    private val frontendUrl: String,
) {

    @Bean
    fun corsConfigurationSource() =
        UrlBasedCorsConfigurationSource().apply {

            registerCorsConfiguration(
                "/**",
                CorsConfiguration().apply {
                    allowedOrigins = listOf(
                        "http://localhost:3000",
                        frontendUrl,
                    ).distinct()
                    allowedMethods = listOf(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS",
                    )
                    allowedHeaders = listOf("*")
                    allowCredentials = true
                }
            )
        }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .cors {
                it.configurationSource(corsConfigurationSource())
            }

            .csrf {
                it.disable()
            }

            .exceptionHandling {
                // 인증되지 않은 요청: 401
                it.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write(
                        """{"message":"인증이 필요합니다."}"""
                    )
                }
                // 인증은 됐지만 역할(Role)이 부족한 경우: 403
                it.accessDeniedHandler { _, response, _ ->
                    response.status = HttpStatus.FORBIDDEN.value()
                    response.contentType = "application/json;charset=UTF-8"
                    response.writer.write(
                        """{"message":"접근 권한이 없습니다."}"""
                    )
                }
            }

            .authorizeHttpRequests {

                it
                    // 인증 불필요
                    .requestMatchers(
                        "/api/v1/auth/signup",
                        "/api/v1/auth/login",
                        "/api/v1/auth/reissue",
                        "/api/v1/auth/logout",
                        "/oauth2/**",
                        "/login/oauth2/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/prometheus",
                        "/actuator/**",
                        "/uploads/**",
                    ).permitAll()

                    // 마라톤
                    // 목록·상세 조회: 비회원도 열람 가능
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/marathons",
                        "/api/v1/marathons/{id:\\d+}",
                    ).permitAll()

                    // 내가 등록한 마라톤 조회: 로그인 필요
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/marathons/me",
                    ).authenticated()

                    // 마라톤 등록: 주최자·관리자만 가능
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/marathons",
                    ).hasAnyRole("ORGANIZER", "ADMIN")

                    // 마라톤 취소: 인증된 사용자
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/v1/marathons/*/cancel",
                    ).authenticated()

                    // 마라톤 부분 수정: 주최자·관리자만 가능
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/v1/marathons/{marathonId:\\d+}",
                    ).hasAnyRole("ORGANIZER", "ADMIN")

                    // 마라톤 전체 수정: 주최자·관리자만 가능
                    .requestMatchers(
                        HttpMethod.PUT,
                        "/api/v1/marathons/*",
                    ).hasAnyRole("ORGANIZER", "ADMIN")

                    // 마라톤 삭제: 주최자·관리자만 가능
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/v1/marathons/*",
                    ).hasAnyRole("ORGANIZER", "ADMIN")

                    // 주최자 접수 관리
                    // 접수 요약 조회: 주최자·관리자만 가능
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/organizer/marathons/*/registrations/summary",
                    ).hasAnyRole("ORGANIZER", "ADMIN")

                    // 접수 목록 조회: 주최자·관리자만 가능
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/organizer/marathons/*/registrations",
                    ).hasAnyRole("ORGANIZER", "ADMIN")

                    // 접수 상세 조회: 주최자·관리자만 가능
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/organizer/marathons/*/registrations/*",
                    ).hasAnyRole("ORGANIZER", "ADMIN")

                    // 내 접수
                    // 내 접수 내역 조회: 로그인 필요
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/registrations/me",
                    ).authenticated()

                    // 접수 신청: 로그인 필요
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/registrations",
                    ).authenticated()

                    // 접수 취소: 로그인 필요, 본인 접수 여부는 서비스 레이어에서 검증
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/v1/registrations/*",
                    ).authenticated()

                    // 결제 승인: 로그인 필요
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/payments/confirm",
                    ).authenticated()

                    // 관리자
                    // /api/v1/admin/** 전체: ADMIN 역할만 접근 가능
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")

                    .anyRequest()
                    .authenticated()
            }

            .oauth2Login {
                it.userInfoEndpoint { userInfo ->
                    userInfo.userService(customOAuth2UserService)
                }
                it.successHandler(oAuth2AuthenticationSuccessHandler)
                it.failureHandler(oAuth2AuthenticationFailureHandler)
            }

            .addFilterBefore(
                customAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )

            .build()
}
