package com.rungo.api.domain.registration.queue

object RegistrationQueueKeyGenerator {

    fun activeCourses(): String = ACTIVE_COURSES_KEY

    /**
     * 현재 대기 중인 접수 요청 requestId들을 코스별 순서대로 저장하는 waiting queue 키
     */
    fun waitingQueue(courseId: Long): String = "$QUEUE_PREFIX:course:$courseId:waiting"

    /**
     * requestId 기준 실제 접수 요청 본문을 저장하는 payload 키
     */
    fun payload(requestId: String): String = "$QUEUE_PREFIX:payload:$requestId"

    /**
     * requestId 기준 처리 결과를 저장하는 result 키
     */
    fun result(requestId: String): String = "$QUEUE_PREFIX:result:$requestId"

    /**
     * 현재 처리 중인 requestId들을 TTL과 함께 저장하는 processing 집합 키
     */
    fun processingRequests(): String = PROCESSING_REQUESTS_KEY

    /**
     * 같은 사용자와 같은 코스 조합의 중복 enqueue를 잠시 막기 위한 dedupe 키
     */
    fun dedupe(userId: Long, courseId: Long): String =
        "$QUEUE_PREFIX:dedupe:user:$userId:course:$courseId"

    private const val QUEUE_PREFIX = "queue:registration"

    private const val ACTIVE_COURSES_KEY = "$QUEUE_PREFIX:active:courses"
    private const val PROCESSING_REQUESTS_KEY = "$QUEUE_PREFIX:processing:requests"
}
