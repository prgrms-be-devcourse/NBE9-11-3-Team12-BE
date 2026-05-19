package com.rungo.api.domain.registration.queue.repository

import com.rungo.api.domain.registration.queue.RegistrationQueueKeyGenerator
import com.rungo.api.domain.registration.queue.config.RegistrationQueueProperties
import com.rungo.api.domain.registration.queue.dto.RegistrationQueuePayload
import com.rungo.api.domain.registration.queue.dto.RegistrationQueueResult
import org.redisson.api.RBucket
import org.redisson.api.RScoredSortedSet
import org.redisson.api.RSet
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.redisson.codec.JsonJacksonCodec
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RegistrationQueueRepository(
    private val redissonClient: RedissonClient,
    private val properties: RegistrationQueueProperties
) {

    fun addActiveCourse(courseId: Long): Boolean = activeCoursesSet().add(courseId.toString())

    fun removeActiveCourse(courseId: Long): Boolean = activeCoursesSet().remove(courseId.toString())

    fun findActiveCourseIds(): Set<Long> = activeCoursesSet()
        .readAll()
        .map(String::toLong)
        .toSet()

    fun enqueue(courseId: Long, requestId: String, score: Double): Boolean =
        waitingQueue(courseId).add(score, requestId)

    fun peekWaitingRequestId(courseId: Long): String? = waitingQueue(courseId).first()

    fun removeWaitingRequest(courseId: Long, requestId: String): Boolean =
        waitingQueue(courseId).remove(requestId)

    fun findWaitingRequestIds(courseId: Long): List<String> = waitingQueue(courseId).readAll().toList()

    fun removeWaitingRequests(courseId: Long, requestIds: Collection<String>): Boolean {
        if (requestIds.isEmpty()) {
            return false
        }

        return waitingQueue(courseId).removeAll(requestIds)
    }

    fun waitingSize(courseId: Long): Int = waitingQueue(courseId).size()

    fun savePayload(requestId: String, payload: RegistrationQueuePayload) {
        payloadBucket(requestId).set(payload, Duration.ofMinutes(properties.payloadTtl))
    }

    fun findPayload(requestId: String): RegistrationQueuePayload? = payloadBucket(requestId).get()

    fun deletePayload(requestId: String): Boolean = payloadBucket(requestId).delete()

    fun saveResult(requestId: String, result: RegistrationQueueResult) {
        resultBucket(requestId).set(result, Duration.ofMinutes(properties.resultTtl))
    }

    fun findResult(requestId: String): RegistrationQueueResult? = resultBucket(requestId).get()

    fun deleteResult(requestId: String): Boolean = resultBucket(requestId).delete()

    fun tryMarkProcessing(requestId: String): Boolean =
        processingBucket(requestId).setIfAbsent(PROCESSING_VALUE, Duration.ofSeconds(properties.processingTtl))

    fun isProcessing(requestId: String): Boolean = processingBucket(requestId).isExists

    fun deleteProcessing(requestId: String): Boolean = processingBucket(requestId).delete()

    fun trySetDedupe(userId: Long, marathonId: Long, requestId: String, ttlMinutes: Long): Boolean =
        dedupeBucket(userId, marathonId).setIfAbsent(requestId, Duration.ofMinutes(ttlMinutes))

    fun existsDedupe(userId: Long, marathonId: Long): Boolean =
        dedupeBucket(userId, marathonId).isExists

    fun deleteDedupe(userId: Long, marathonId: Long): Boolean =
        dedupeBucket(userId, marathonId).delete()

    private fun activeCoursesSet(): RSet<String> =
        redissonClient.getSet(RegistrationQueueKeyGenerator.activeCourses(), StringCodec.INSTANCE)

    private fun waitingQueue(courseId: Long): RScoredSortedSet<String> =
        redissonClient.getScoredSortedSet(
            RegistrationQueueKeyGenerator.waitingQueue(courseId),
            StringCodec.INSTANCE
        )

    private fun payloadBucket(requestId: String): RBucket<RegistrationQueuePayload> =
        redissonClient.getBucket(RegistrationQueueKeyGenerator.payload(requestId), JSON_CODEC)

    private fun resultBucket(requestId: String): RBucket<RegistrationQueueResult> =
        redissonClient.getBucket(RegistrationQueueKeyGenerator.result(requestId), JSON_CODEC)

    private fun processingBucket(requestId: String): RBucket<String> =
        redissonClient.getBucket(RegistrationQueueKeyGenerator.processing(requestId), StringCodec.INSTANCE)

    private fun dedupeBucket(userId: Long, marathonId: Long): RBucket<String> =
        redissonClient.getBucket(
            RegistrationQueueKeyGenerator.dedupe(userId, marathonId),
            StringCodec.INSTANCE
        )

    companion object {
        private const val PROCESSING_VALUE = "PROCESSING"
        private val JSON_CODEC = JsonJacksonCodec()
    }
}
