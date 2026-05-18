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

        val cookies = request.cookies

        cookies?.forEach { cookie ->

            if (cookie.name == "accessToken") {

                val token = cookie.value

                if (JwtUtil.validateToken(token, jwtSecret)) {

                    val claims = JwtUtil.getClaims(token, jwtSecret)

                    val id = (claims["id"] as Number).toLong()
                    val email = claims.get("email", String::class.java)
                    val roleStr = claims.get("role", String::class.java)

                    val role = Role.valueOf(roleStr)

                    val authorities = listOf(
                        SimpleGrantedAuthority("ROLE_${role.name}")
                    )

                    val securityUser = SecurityUser(
                        id,
                        email,
                        role,
                        authorities
                    )

                    val authentication =
                        UsernamePasswordAuthenticationToken(
                            securityUser,
                            null,
                            authorities
                        )

                    SecurityContextHolder.getContext()
                        .authentication = authentication
                }

//                else {
//                    response.status = HttpServletResponse.SC_UNAUTHORIZED
//                    response.contentType = "application/json;charset=UTF-8"
//                    response.writer.write(
//                        """{"message":"유효하지 않은 액세스 토큰입니다."}"""
//                    )
//                    return
//                }
            }
        }

        filterChain.doFilter(request, response)
    }
}