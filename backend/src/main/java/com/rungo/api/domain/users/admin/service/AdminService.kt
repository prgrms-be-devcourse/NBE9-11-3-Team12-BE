package com.rungo.api.domain.users.admin.service

import com.rungo.api.domain.users.admin.dto.AdminApproveRes
import com.rungo.api.domain.users.admin.dto.RejectOrganizerApplicationReq
import com.rungo.api.domain.users.admin.dto.RejectOrganizerApplicationRes
import com.rungo.api.domain.users.admin.dto.AdminOrganizerApplicationListRes
import com.rungo.api.domain.users.enumtype.Role
import com.rungo.api.domain.users.organizerApplication.repository.OrganizerApplicationRepository
import com.rungo.api.domain.users.organizerApplication.status.ApplicationStatus
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val organizerApplicationRepository: OrganizerApplicationRepository,
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
    @Transactional
    fun rejectOrganizerApplication(
        adminId: Long,
        applicationId: Long,
        req: RejectOrganizerApplicationReq,
    ): RejectOrganizerApplicationRes {
        val admin = userRepository.findByIdOrNull(adminId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (admin.role != Role.ADMIN) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val application = organizerApplicationRepository.findByIdOrNull(applicationId)
            ?: throw CustomException(ErrorCode.ORGANIZER_APPLICATION_NOT_FOUND)

        if (application.status != ApplicationStatus.PENDING) {
            throw CustomException(ErrorCode.ALREADY_PROCESSED_APPLICATION)
        }

        application.reject(req.rejectReason)

        return RejectOrganizerApplicationRes.from(application)
    }

    @Transactional(readOnly = true)
    fun getOrganizerApplications(
        adminId: Long,
        status: ApplicationStatus?,
        pageable: Pageable,
    ): AdminOrganizerApplicationListRes {
        val admin = userRepository.findByIdOrNull(adminId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (admin.role != Role.ADMIN) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val sortedPageable = PageRequest.of(
            pageable.pageNumber,
            pageable.pageSize,
            Sort.by(
                Sort.Order.desc("requestedAt"),
                Sort.Order.desc("id"),
            )
        )

        val page = status?.let {
            organizerApplicationRepository.findAllByStatus(it, sortedPageable)
        } ?: organizerApplicationRepository.findAll(sortedPageable)

        return AdminOrganizerApplicationListRes.from(page)
    }
}
