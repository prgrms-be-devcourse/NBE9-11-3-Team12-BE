package com.rungo.api.domain.registration.queue.repository

import com.rungo.api.domain.registration.queue.RegistrationQueueKeyGenerator
import com.rungo.api.domain.registration.queue.dto.RegistrationQueuePayload
import com.rungo.api.domain.registration.queue.dto.RegistrationQueueResult
import com.rungo.api.domain.registration.dto.CreateRegistrationRes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class RegistrationQueueRepositoryTest {

    @Autowired
    private lateinit var registrationQueueRepository: RegistrationQueueRepository

    @Autowired
    private lateinit var redissonClient: RedissonClient

    @AfterEach
    fun tearDown() {
        redissonClient.keys.deleteByPattern("queue:registration:*")
    }

    @Test
    @DisplayName("대기열 키 생성 성공 - 코스별 waiting 키와 payload 키, dedupe 키를 올바르게 만든다")
    fun key_generator_success() {
        assertThat(RegistrationQueueKeyGenerator.waitingQueue(1L))
            .isEqualTo("queue:registration:course:1:waiting")
        assertThat(RegistrationQueueKeyGenerator.payload("request-1"))
            .isEqualTo("queue:registration:payload:request-1")
        assertThat(RegistrationQueueKeyGenerator.dedupe(2L, 3L))
            .isEqualTo("queue:registration:dedupe:user:2:marathon:3")
    }

    @Test
    @DisplayName("대기열 저장 성공 - enqueue 후 score가 가장 작은 요청이 먼저 조회되고 제거 후 다음 요청이 조회된다")
    fun waiting_queue_success() {
        val courseId = 1L

        registrationQueueRepository.enqueue(courseId, "request-2", 2.0)
        registrationQueueRepository.enqueue(courseId, "request-1", 1.0)

        assertThat(registrationQueueRepository.peekWaitingRequestId(courseId)).isEqualTo("request-1")
        assertThat(registrationQueueRepository.waitingSize(courseId)).isEqualTo(2)

        registrationQueueRepository.removeWaitingRequest(courseId, "request-1")

        assertThat(registrationQueueRepository.peekWaitingRequestId(courseId)).isEqualTo("request-2")
        assertThat(registrationQueueRepository.waitingSize(courseId)).isEqualTo(1)
        assertThat(registrationQueueRepository.findWaitingRequestIds(courseId))
            .containsExactly("request-2")
    }

    @Test
    @DisplayName("대기열 일괄 제거 성공 - 지정한 requestId 목록을 한 번에 제거할 수 있다")
    fun waiting_queue_batch_remove_success() {
        val courseId = 1L

        registrationQueueRepository.enqueue(courseId, "request-1", 1.0)
        registrationQueueRepository.enqueue(courseId, "request-2", 2.0)

        assertThat(
            registrationQueueRepository.removeWaitingRequests(courseId, listOf("request-1", "request-2"))
        ).isTrue()
        assertThat(registrationQueueRepository.waitingSize(courseId)).isZero()
    }

    @Test
    @DisplayName("payload 저장 성공 - requestId 기준으로 payload를 저장하고 다시 조회할 수 있다")
    fun payload_save_and_find_success() {
        val requestId = "request-payload"
        val payload = RegistrationQueuePayload(
            userId = 1L,
            marathonId = 2L,
            courseId = 3L,
            snapZipCode = "12345",
            snapAddress = "서울시 강남구",
            snapDetail = "101동",
            tSize = "L",
            agreedTerms = true
        )

        registrationQueueRepository.savePayload(requestId, payload)

        assertThat(registrationQueueRepository.findPayload(requestId)).isEqualTo(payload)
    }

    @Test
    @DisplayName("result 저장 성공 - requestId 기준으로 처리 결과를 저장하고 다시 조회할 수 있다")
    fun result_save_and_find_success() {
        val requestId = "request-result"
        val result = RegistrationQueueResult(
            success = true,
            response = CreateRegistrationRes(
                registrationId = 1L,
                marathonId = 2L,
                marathonTitle = "서울 마라톤",
                courseId = 3L,
                courseType = "10K",
                status = "COMPLETED",
                appliedAt = LocalDateTime.of(2026, 5, 19, 12, 0)
            )
        )

        registrationQueueRepository.saveResult(requestId, result)

        assertThat(registrationQueueRepository.findResult(requestId)).isEqualTo(result)
    }

    @Test
    @DisplayName("processing 마킹 성공 - 같은 requestId는 한 번만 processing에 등록할 수 있다")
    fun processing_mark_success() {
        val requestId = "request-processing"

        assertThat(registrationQueueRepository.tryMarkProcessing(requestId)).isTrue()
        assertThat(registrationQueueRepository.tryMarkProcessing(requestId)).isFalse()
        assertThat(registrationQueueRepository.isProcessing(requestId)).isTrue()
        assertThat(registrationQueueRepository.processingCount()).isEqualTo(1)

        registrationQueueRepository.deleteProcessing(requestId)

        assertThat(registrationQueueRepository.isProcessing(requestId)).isFalse()
        assertThat(registrationQueueRepository.processingCount()).isZero()
    }

    @Test
    @DisplayName("dedupe 저장 성공 - 같은 사용자와 같은 마라톤 조합은 한 번만 저장할 수 있다")
    fun dedupe_try_set_and_exists_success() {
        val userId = 1L
        val marathonId = 2L

        assertThat(registrationQueueRepository.trySetDedupe(userId, marathonId, "request-1", 5)).isTrue()
        assertThat(registrationQueueRepository.trySetDedupe(userId, marathonId, "request-2", 5)).isFalse()
        assertThat(registrationQueueRepository.existsDedupe(userId, marathonId)).isTrue()

        registrationQueueRepository.deleteDedupe(userId, marathonId)

        assertThat(registrationQueueRepository.existsDedupe(userId, marathonId)).isFalse()
    }

    @Test
    @DisplayName("활성 코스 저장 성공 - active course를 추가하고 조회 후 삭제할 수 있다")
    fun active_course_add_and_remove_success() {
        val courseId = 1L

        assertThat(registrationQueueRepository.addActiveCourse(courseId)).isTrue()
        assertThat(registrationQueueRepository.findActiveCourseIds()).containsExactly(courseId)

        registrationQueueRepository.removeActiveCourse(courseId)

        assertThat(registrationQueueRepository.findActiveCourseIds()).isEmpty()
    }
}
