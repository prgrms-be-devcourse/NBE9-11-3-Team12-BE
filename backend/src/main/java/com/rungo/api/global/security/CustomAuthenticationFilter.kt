package com.rungo.api.global.security

import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.global.util.JwtUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CustomAuthenticationFilter(

    @Value("\${jwt.secret}")
    private val jwtSecret: String

) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val token = request.cookies
            ?.find { it.name == "accessToken" }
            ?.value

        if (token != null && JwtUtil.validateToken(token, jwtSecret)) {

            val claims = JwtUtil.getClaims(token, jwtSecret)
            val id = (claims["id"] as Number).toLong()
            val email = claims.get("email", String::class.java)
            val role = Role.valueOf(claims.get("role", String::class.java))
            val authorities = listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
            val securityUser = SecurityUser(id, email, role, authorities)

            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(securityUser, null, authorities)
        }

        filterChain.doFilter(request, response)
    }
}