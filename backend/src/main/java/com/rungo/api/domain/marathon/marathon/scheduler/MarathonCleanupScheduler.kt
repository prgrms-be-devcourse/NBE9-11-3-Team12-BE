package com.rungo.api.domain.marathon.marathon.scheduler

import com.rungo.api.domain.marathon.marathon.service.MarathonCleanupService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MarathonCleanupScheduler(
    private val marathonCleanupService: MarathonCleanupService,
) {

    @Scheduled(cron = "0 0 3 * * *") // 매일 오전 3시
    fun cleanup() {
        marathonCleanupService.cleanup()
    }
}