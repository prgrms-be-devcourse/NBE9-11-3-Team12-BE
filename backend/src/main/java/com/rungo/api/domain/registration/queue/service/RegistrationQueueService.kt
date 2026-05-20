package com.rungo.api.domain.registration.queue.service

import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.dto.CreateRegistrationRes
import com.rungo.api.domain.registration.queue.config.RegistrationQueueProperties
import com.rungo.api.domain.registration.queue.dto.RegistrationQueuePayload
import com.rungo.api.domain.registration.queue.dto.RegistrationQueueResult
import com.rungo.api.domain.registration.queue.repository.RegistrationQueueRepository
import com.rungo.api.domain.registration.service.RegistrationService
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.DataIntegrityViolationErrorCodeResolver
import com.rungo.api.global.exception.ErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.util.*

@Service
class RegistrationQueueService(
    private val registrationQueueRepository: RegistrationQueueRepository,
    private val registrationService: RegistrationService,
    private val properties: RegistrationQueueProperties
) {

    // 요청을 먼저 대기열에 넣고, 처리 결과가 나올 때까지 기다린다.
    fun create(userId: Long, request: CreateRegistrationReq): CreateRegistrationRes {
        val requestId = enqueue(userId, request)


        return awaitResult(requestId)
    }

    // 접수 요청 원본을 저장한 뒤 코스별 대기열에 requestId를 넣는다.
    fun enqueue(userId: Long, request: CreateRegistrationReq): String {
        val courseId = request.courseId
        val requestId = UUID.randomUUID().toString()

        // 같은 사용자와 같은 코스 요청은 한 번만 대기열에 넣는다.
        if (!registrationQueueRepository.trySetDedupe(userId, courseId, requestId, 5)) {
            throw CustomException(ErrorCode.REGISTRATION_ALREADY_EXISTS)
        }

        val payload = RegistrationQueuePayload(
            userId = userId,
            courseId = courseId,
            snapZipCode = request.snapZipCode,
            snapAddress = request.snapAddress,
            snapDetail = request.snapDetail,
            tSize = request.tSize,
            agreedTerms = request.agreedTerms
        )

        try {
            registrationQueueRepository.savePayload(requestId, payload)
            val enqueued = registrationQueueRepository.enqueue(courseId, requestId, createQueueScore())
            if (!enqueued) {
                throw IllegalStateException("registration queue enqueue failed")
            }
            registrationQueueRepository.addActiveCourse(courseId)
        } catch (e: RuntimeException) {
            // 중간에 실패하면 대기열에 남은 흔적을 지워 다시 시도할 수 있게 한다.
            registrationQueueRepository.removeWaitingRequest(courseId, requestId)
            registrationQueueRepository.deletePayload(requestId)
            registrationQueueRepository.deleteDedupe(userId, courseId)
            throw e
        }

        return requestId
    }

    // 스케줄러가 꺼낸 요청을 실제 접수 생성으로 처리한다.
    fun process(requestId: String) {
        val payload = registrationQueueRepository.findPayload(requestId) ?: run {
            registrationQueueRepository.deleteProcessing(requestId)
            return
        }

        try {
            val response = registrationService.create(payload.userId, payload.toCreateRegistrationReq())
            registrationQueueRepository.saveResult(requestId, RegistrationQueueResult(success = true, response = response))
        } catch (e: Exception) {
            // 실패 원인을 저장해 두었다가 create() 쪽에서 다시 예외로 복원한다.
            val errorCode = when {
                e is CustomException -> e.errorCode.name
                e is DataIntegrityViolationException ->
                    (DataIntegrityViolationErrorCodeResolver.resolve(e) ?: ErrorCode.INTERNAL_SERVER_ERROR).name

                else -> ErrorCode.INTERNAL_SERVER_ERROR.name
            }
            registrationQueueRepository.saveResult(
                requestId,
                RegistrationQueueResult(success = false, errorCode = errorCode)
            )
        } finally {
            registrationQueueRepository.deleteProcessing(requestId)
            registrationQueueRepository.deletePayload(requestId)
            registrationQueueRepository.deleteDedupe(payload.userId, payload.courseId)
        }
    }

    private fun createQueueScore(): Double = System.currentTimeMillis().toDouble()

    // (임시) 사용자에게는 접수 완료 응답이 바로 보이도록 여기서 결과를 기다린다.
    private fun awaitResult(requestId: String): CreateRegistrationRes {
        val deadline = System.currentTimeMillis() + (properties.processingTtl * 1000)

        while (System.currentTimeMillis() < deadline) {
            val result = registrationQueueRepository.findResult(requestId)
            if (result != null) {
                registrationQueueRepository.deleteResult(requestId)

                if (result.success) {
                    return result.response ?: throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
                }

                throw CustomException(resolveErrorCode(result.errorCode))
            }

            Thread.sleep(properties.pollInterval)
        }

        throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    // 저장해 둔 에러 코드를 다시 서비스 예외로 바꾼다.
    private fun resolveErrorCode(errorCode: String?): ErrorCode {
        if (errorCode == null) {
            return ErrorCode.INTERNAL_SERVER_ERROR
        }

        return try {
            ErrorCode.valueOf(errorCode)
        } catch (e: IllegalArgumentException) {
            ErrorCode.INTERNAL_SERVER_ERROR
        }
    }

    private fun RegistrationQueuePayload.toCreateRegistrationReq(): CreateRegistrationReq = CreateRegistrationReq(
        courseId = courseId,
        snapZipCode = snapZipCode,
        snapAddress = snapAddress,
        snapDetail = snapDetail,
        tSize = tSize,
        agreedTerms = agreedTerms
    )
}
