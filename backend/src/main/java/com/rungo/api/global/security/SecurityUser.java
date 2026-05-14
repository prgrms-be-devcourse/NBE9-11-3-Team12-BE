package com.rungo.api.global.security;

import com.rungo.api.domain.users.enumtype.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class SecurityUser extends User { // 인증된 사용자 정보

    private Long id;
    private String email;
    private Role role;

    public SecurityUser(Long id, String email, Role role, Collection<? extends GrantedAuthority> authorities) {
        super(email, "password", authorities); // 인증된 사용자의 password값은 필요 없으므로, 문자열 고정
        this.id = id;
        this.email = email;
        this.role = role;
    }
}