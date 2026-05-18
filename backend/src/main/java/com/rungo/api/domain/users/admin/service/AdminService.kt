package com.rungo.api.domain.users.admin.service

import com.rungo.api.domain.users.admin.dto.AdminApproveRes
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val userRepository: UserRepository
) {


    @Transactional
    fun approveOrganizer(adminId: Long, userId: Long): AdminApproveRes {
        val admin = userRepository.findByIdOrNull(adminId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (admin.role != Role.ADMIN) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val user = userRepository.findByIdOrNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (user.role == Role.ORGANIZER) {
            throw CustomException(ErrorCode.ALREADY_ORGANIZER)
        }

        user.promoteToOrganizer()

        return AdminApproveRes(
            user.id,
            user.email,
            user.name,
            user.phoneNumber,
            user.gender,
            user.birth,
            user.role
        )
    }
}
