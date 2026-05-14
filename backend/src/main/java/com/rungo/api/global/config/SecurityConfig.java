package com.rungo.api.global.config;

import com.rungo.api.domain.auth.handler.OAuth2AuthenticationFailureHandler;
import com.rungo.api.domain.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.rungo.api.domain.auth.service.CustomOAuth2UserService;
import com.rungo.api.global.security.CustomAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration =new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // CORS 설정 적용

        return source;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // exceptionHandling 적용
                .exceptionHandling(ex -> ex
                        // 인증이 안 됐을 경우 401
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\": \"인증이 필요합니다.\"}");
                        })
                        // 인증은 됐지만 권한이 부족한 경우 403
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\": \"접근 권한이 없습니다.\"}");
                        })
                )

                .authorizeHttpRequests(auth -> auth

                        // AUTH, SWAGGER: 인증 없이 접근 허용
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
                                "/actuator/**"
                        ).permitAll()

                        // 마라톤
                        // 목록/상세 조회: 인증 없이 허용 (숫자 ID만, /me 제외)
                        .requestMatchers(HttpMethod.GET, "/api/v1/marathons").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/marathons/{id:\\d+}").permitAll()

                        // 내 마라톤 조회: 인증 필요
                        .requestMatchers(HttpMethod.GET, "/api/v1/marathons/me").authenticated()

                        // 마라톤 등록: ORGANIZER, ADMIN만
                        .requestMatchers(HttpMethod.POST, "/api/v1/marathons")
                        .hasAnyRole("ORGANIZER", "ADMIN")

                        // 마라톤 취소: 인증된 사용자
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/marathons/*/cancel")
                        .authenticated()

                        // 마라톤 수정: ORGANIZER, ADMIN만
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/marathons/{marathonId:\\d+}")
                        .hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/marathons/*")
                        .hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/marathons/*")
                        .hasAnyRole("ORGANIZER", "ADMIN")

                        // 접수
                        // 주최자 접수 현황/목록/상세 조회: ORGANIZER, ADMIN만
                        .requestMatchers(HttpMethod.GET, "/api/v1/organizer/marathons/*/registrations/summary")
                        .hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/organizer/marathons/*/registrations")
                        .hasAnyRole("ORGANIZER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/organizer/marathons/*/registrations/*")
                        .hasAnyRole("ORGANIZER", "ADMIN")

                        // 내 접수 목록 조회: 인증된 사용자
                        .requestMatchers(HttpMethod.GET, "/api/v1/registrations/me")
                        .authenticated()

                        // 접수 등록/취소: 인증된 사용자
                        .requestMatchers(HttpMethod.POST, "/api/v1/registrations")
                        .authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/registrations/*")
                        .authenticated()

                        // 관리자
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // 그 외: 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}