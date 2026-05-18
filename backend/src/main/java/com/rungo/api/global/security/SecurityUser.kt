package com.rungo.api.global.security

import com.rungo.api.domain.users.enumtype.Role
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.User

class SecurityUser(// 인증된 사용자 정보
    val id: Long,
    val email: String,
    val role: Role,
    authorities: Collection<GrantedAuthority>
) : User(email, "password", authorities)