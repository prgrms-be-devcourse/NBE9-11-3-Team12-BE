package com.rungo.api.domain.users.organizerApplication.service

import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.organizerApplication.dto.OrganizerApplicationCreateReq
import com.rungo.api.domain.users.organizerApplication.dto.OrganizerApplicationCreateRes
import com.rungo.api.domain.users.organizerApplication.entity.OrganizerApplication
import com.rungo.api.domain.users.organizerApplication.repository.OrganizerApplicationRepository
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrganizerApplicationService(
    private val userRepository: UserRepository,
    private val organizerApplicationRepository: OrganizerApplicationRepository,
) {

    @Transactional
    fun requestApplication(
        userId: Long,
        req: OrganizerApplicationCreateReq,
    ): OrganizerApplicationCreateRes {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (user.role == Role.ORGANIZER) {
            throw CustomException(ErrorCode.ALREADY_ORGANIZER)
        }

        val hasPendingApplication = organizerApplicationRepository.existsByUserIdAndStatus(
            userId = userId,
            status = ApplicationStatus.PENDING,
        )

        if (hasPendingApplication) {
            throw CustomException(ErrorCode.ALREADY_PENDING_APPLICATION)
        }

        val application = OrganizerApplication.create(
            user = user,
            businessRegistrationNumber = req.businessRegistrationNumber,
        )

        return OrganizerApplicationCreateRes.from(
            organizerApplicationRepository.save(application)
        )
    }
}