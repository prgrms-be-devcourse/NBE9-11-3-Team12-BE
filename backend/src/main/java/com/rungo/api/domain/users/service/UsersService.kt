package com.rungo.api.domain.users.service

import com.rungo.api.domain.users.dto.CompleteProfileReq
import com.rungo.api.domain.users.dto.MyProfileRes
import com.rungo.api.domain.users.dto.UpdateMyProfileReq
import com.rungo.api.domain.users.dto.UpdateMyProfileRes
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class UsersService(
    private val usersRepository: UserRepository
) {

    fun getMyInfo(userId: Long): MyProfileRes {
        val user = usersRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }

        return MyProfileRes(
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
    fun updateMyProfile(userId: Long, req: UpdateMyProfileReq): UpdateMyProfileRes {
        val user = usersRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }

        user.updateProfile(
            req.name ?: user.name,
            req.phoneNumber ?: user.phoneNumber,
            req.gender ?: user.gender,
            req.birth ?: user.birth,
        )

        return UpdateMyProfileRes(
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
    fun completeMyProfile(userId: Long, req: CompleteProfileReq) {
        val user = usersRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }

        user.updateProfile(
            req.name,
            req.phoneNumber,
            req.gender,
            req.birth
        )
    }
}