package com.rungo.api.domain.auth.dto;

import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Role;

public record MeRes(
        Long id,
        String email,
        String name,
        Role role,
        boolean profileCompleted
) {
    public static MeRes from(Users user) {
        return new MeRes(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isProfileCompleted()
        );
    }
}