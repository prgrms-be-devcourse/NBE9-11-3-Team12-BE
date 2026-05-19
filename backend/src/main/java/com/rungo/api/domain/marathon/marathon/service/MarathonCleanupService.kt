package com.rungo.api.domain.marathon.marathon.service

import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MarathonCleanupService(
    private val marathonRepository: MarathonRepository,
    private val registrationRepository: RegistrationRepository,
    private val registrationCancelHistoryRepository: RegistrationCancelHistoryRepository,
) {

    @Transactional
    fun cleanup() {
        val now = LocalDate.now()

        // 5년 이상 지난 대회들의 ID 조회 후, 취소 이력 → 참가 신청 → 대회 순으로 삭제
        val fiveYearsAgoIds = marathonRepository.findIdsByEventDateBefore(now.minusYears(5))
        if (fiveYearsAgoIds.isNotEmpty()) {
            registrationCancelHistoryRepository.deleteAllByMarathonIdIn(fiveYearsAgoIds)
            registrationRepository.deleteAllByMarathonIdIn(fiveYearsAgoIds)
            val marathons = marathonRepository.findAllById(fiveYearsAgoIds)
            marathonRepository.deleteAll(marathons)
        }
    }
}