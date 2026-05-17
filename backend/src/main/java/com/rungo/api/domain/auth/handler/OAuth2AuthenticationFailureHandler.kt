package com.rungo.api.domain.auth.handler

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class OAuth2AuthenticationFailureHandler : AuthenticationFailureHandler {
    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse,
        exception: AuthenticationException?
    ) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 Login Failed")
    }
}