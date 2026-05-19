package com.rungo.api.domain.registration.queue.service

import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.queue.dto.RegistrationQueuePayload
import com.rungo.api.domain.registration.queue.repository.RegistrationQueueRepository
import com.rungo.api.global.exception.CustomException
import com.rungo.api.global.exception.ErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RegistrationQueueService(
    private val registrationQueueRepository: RegistrationQueueRepository,
    private val courseRepository: CourseRepository
) {

    fun enqueue(userId: Long, request: CreateRegistrationReq): String {
        val course = courseRepository.findByIdOrNull(request.courseId)
            ?: throw CustomException(ErrorCode.COURSE_NOT_FOUND)

        val requestId = UUID.randomUUID().toString()
        val payload = RegistrationQueuePayload(
            userId = userId,
            marathonId = course.marathon.id,
            courseId = course.id,
            snapZipCode = request.snapZipCode,
            snapAddress = request.snapAddress,
            snapDetail = request.snapDetail,
            tSize = request.tSize,
            agreedTerms = request.agreedTerms
        )

        registrationQueueRepository.savePayload(requestId, payload)
        registrationQueueRepository.enqueue(course.id, requestId, createQueueScore())
        registrationQueueRepository.addActiveCourse(course.id)

        return requestId
    }

    private fun createQueueScore(): Double = System.currentTimeMillis().toDouble()
}
