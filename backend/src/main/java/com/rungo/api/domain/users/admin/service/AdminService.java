package com.rungo.api.domain.users.admin.service;

import com.rungo.api.domain.users.admin.dto.AdminApproveRes;
import com.rungo.api.domain.users.dto.MyProfileRes;
import com.rungo.api.domain.users.entity.Users;
import com.rungo.api.domain.users.enumtype.Role;
import com.rungo.api.domain.users.repository.UserRepository;
import com.rungo.api.global.exception.CustomException;
import com.rungo.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;

    @Transactional
    public AdminApproveRes approveOrganizer(Long adminId, Long userId) {

        Users admin = userRepository.findById(adminId)

                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (admin.getRole() != Role.ADMIN) {

            throw new CustomException(ErrorCode.FORBIDDEN);

        }

        Users user = userRepository.findById(userId)

                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == Role.ORGANIZER) {

            throw new CustomException(ErrorCode.ALREADY_ORGANIZER);

        }

        user.promoteToOrganizer();

        return new AdminApproveRes(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getBirth(),
                user.getRole()
        );
    }
}
