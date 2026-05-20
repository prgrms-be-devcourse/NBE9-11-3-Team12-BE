package com.rungo.api.domain.registration.queue.scheduler

import com.rungo.api.domain.registration.queue.config.RegistrationQueueConfig
import com.rungo.api.domain.registration.queue.config.RegistrationQueueProperties
import com.rungo.api.domain.registration.queue.repository.RegistrationQueueRepository
import com.rungo.api.domain.registration.queue.service.RegistrationQueueService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Executor

@Component
@ConditionalOnProperty(
    prefix = "registration.queue",
    name = ["scheduler-enabled"],
    havingValue = "true"
)
class RegistrationQueueScheduler(
    private val properties: RegistrationQueueProperties,
    private val registrationQueueRepository: RegistrationQueueRepository,
    private val registrationQueueService: RegistrationQueueService,
    @Qualifier(RegistrationQueueConfig.REGISTRATION_QUEUE_WORKER_EXECUTOR)
    private val registrationQueueWorkerExecutor: Executor
) {

    @Scheduled(fixedDelayString = "\${registration.queue.poll-interval}")
    fun pollWaitingQueues() {
        // 현재 처리 가능한 슬롯 계산 (최대 동시 처리 수 - 현재 처리 중인 수)
        val availableSlots = properties.workerConcurrency - registrationQueueRepository.processingCount()
        if (availableSlots <= 0) {
            return
        }

        // 현재 대기 중인 요청이 있는 코스 목록 조회
        val activeCourseIds = registrationQueueRepository.findActiveCourseIds()
        if (activeCourseIds.isEmpty()) {
            return
        }

        var claimedCount = 0

        for (courseId in activeCourseIds) {
            // 남은 processing 슬롯만큼 같은 코스에서 연속 claim한다.
            while (claimedCount < availableSlots) {
                // 코스 대기열에서 요청 하나를 가져옴 (없으면 다음 코스로)
                val claimedRequestId = claimNextRequest(courseId) ?: break
                claimedCount++

                log.info(
                    "registration queue request claimed: courseId={}, requestId={}",
                    courseId,
                    claimedRequestId
                )
                registrationQueueWorkerExecutor.execute {
                    registrationQueueService.process(claimedRequestId)
                }
            }
        }
    }

    private fun claimNextRequest(courseId: Long): String? {
        // 대기열 맨 앞 요청 확인 (꺼내지 않고 보기만 함)
        val requestId = registrationQueueRepository.peekWaitingRequestId(courseId) ?: run {
            // 대기열이 비었으면 활성 코스 목록에서 제거
            registrationQueueRepository.removeActiveCourse(courseId)
            return null
        }

        // processing 마킹 시도 (다른 워커가 이미 처리 중이면 스킵)
        if (!registrationQueueRepository.tryMarkProcessing(requestId)) {
            return null
        }

        // 대기열에서 실제로 제거 (실패 시 processing 마킹 롤백)
        if (!registrationQueueRepository.removeWaitingRequest(courseId, requestId)) {
            registrationQueueRepository.deleteProcessing(requestId)
            return null
        }

        // 대기열이 비었으면 활성 코스 목록에서 제거
        if (registrationQueueRepository.waitingSize(courseId) == 0) {
            registrationQueueRepository.removeActiveCourse(courseId)
        }

        return requestId
    }

    companion object {
        private val log = LoggerFactory.getLogger(RegistrationQueueScheduler::class.java)
    }
}
