package com.rungo.api.global.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse

object CookieUtil {
    private val isProd: Boolean =
        System.getenv("SPRING_PROFILES_ACTIVE")
            ?.split(",")
            ?.map { it.trim() }
            ?.contains("prod") == true

    fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        Cookie(name, value).apply {
            path = "/"
            isHttpOnly = true
            secure = isProd
            setAttribute("SameSite", if (isProd) "None" else "Strict")

            if (!isProd) {
                domain = "localhost"
            }

            setMaxAge(maxAge)
        }.also { response.addCookie(it) }
    }

    fun deleteCookie(response: HttpServletResponse, name: String) {
        Cookie(name, null).apply {
            path = "/"
            isHttpOnly = true
            secure = isProd
            setAttribute("SameSite", if (isProd) "None" else "Strict")

            if (!isProd) {
                domain = "localhost"
            }

            maxAge = 0
        }.also { response.addCookie(it) }
    }
}