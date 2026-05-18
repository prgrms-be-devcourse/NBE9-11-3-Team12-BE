package com.rungo.api.global.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse

object CookieUtil {

    fun addCookie(response: HttpServletResponse, name: String, value: String, maxAge: Int) {
        Cookie(name, value).apply {
            path = "/"
            isHttpOnly = true
            domain = "localhost"
            secure = false // 로컬에서는 false로 설정
            setAttribute("SameSite", "Strict")
            setMaxAge(maxAge)
        }.also { response.addCookie(it) }
    }

    fun deleteCookie(response: HttpServletResponse, name: String) {
        Cookie(name, null).apply {
            maxAge = 0
            path = "/"
            isHttpOnly = true
        }.also { response.addCookie(it) }
    }
}