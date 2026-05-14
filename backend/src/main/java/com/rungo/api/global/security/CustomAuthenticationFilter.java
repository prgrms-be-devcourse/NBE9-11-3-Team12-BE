package com.rungo.api.global.security;

import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.global.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class CustomAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {

                    String token = cookie.getValue();

                    if (JwtUtil.validateToken(token, jwtSecret)) {
                        Claims claims = JwtUtil.getClaims(token, jwtSecret);

                        Long id = claims.get("id", Long.class);
                        String email = claims.get("email", String.class);
                        String roleStr = claims.get("role", String.class);

                        Role role = Role.valueOf(roleStr);

                        List<SimpleGrantedAuthority> authorities =
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

                        SecurityUser securityUser = new SecurityUser(
                                id,
                                email,
                                role,
                                authorities
                        );

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        securityUser,
                                        null,
                                        authorities
                                );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }

//                    else {
//                        // 토큰이 있지만 유효하지 않을 경우 401
//                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                        response.setContentType("application/json;charset=UTF-8");
//                        response.getWriter().write("{\"message\": \"유효하지 않은 액세스 토큰입니다.\"}");
//                        return; // 필터 체인 중단
//                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
