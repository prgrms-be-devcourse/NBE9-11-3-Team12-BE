package com.rungo.api.global.security.support

import com.rungo.api.global.security.SecurityUser
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/test")
class TestSecurityController {

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal securityUser: SecurityUser?): ResponseEntity<*> {
        if (securityUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Any>()
        }
        return ResponseEntity.ok(
            mapOf(
                "id" to securityUser.id,
                "email" to securityUser.email,
                "role" to securityUser.role.name
            )
        )
    }
}