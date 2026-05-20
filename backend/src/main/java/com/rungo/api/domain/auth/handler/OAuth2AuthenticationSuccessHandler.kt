package com.rungo.api.domain.auth.handler

import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.util.CookieUtil
import com.rungo.api.global.util.JwtUtil
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2AuthenticationSuccessHandler(
    private val userRepository: UserRepository
) : AuthenticationSuccessHandler {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${app.frontend.url}")
    private lateinit var frontendUrl: String

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oAuth2User = authentication.principal as? OAuth2User
        if (oAuth2User == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid OAuth2 User")
            return
        }

        val email = oAuth2User.attributes["email"] as? String
        if (email == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 email not found")
            return
        }

        val user = userRepository.findByEmail(email)
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found")
            return
        }

        val userId = user.id

        val accessToken = JwtUtil.generateAccessToken(
            userId,
            user.email,
            user.role,
            secret
        )

        val refreshToken = JwtUtil.generateRefreshToken(
            userId,
            user.email,
            secret
        )

        CookieUtil.addCookie(response, "accessToken", accessToken, ACCESS_TOKEN_EXPIRE)
        CookieUtil.addCookie(response, "refreshToken", refreshToken, REFRESH_TOKEN_EXPIRE)

        response.sendRedirect(frontendUrl)
    }

    companion object {
        private const val ACCESS_TOKEN_EXPIRE = 60 * 60
        private const val REFRESH_TOKEN_EXPIRE = 60 * 60 * 24 * 7
    }
}