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
     * requestId 기준 현재 처리 중인 요청임을 표시하는 processing 키
     */
    fun processing(requestId: String): String = "$QUEUE_PREFIX:processing:$requestId"

    /**
     * 같은 사용자와 같은 마라톤 조합의 중복 enqueue를 잠시 막기 위한 dedupe 키
     */
    fun dedupe(userId: Long, marathonId: Long): String =
        "$QUEUE_PREFIX:dedupe:user:$userId:marathon:$marathonId"

    private const val QUEUE_PREFIX = "queue:registration"

    private const val ACTIVE_COURSES_KEY = "$QUEUE_PREFIX:active:courses"
}
