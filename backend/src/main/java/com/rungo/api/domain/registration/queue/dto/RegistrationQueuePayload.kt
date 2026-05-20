package com.rungo.api.domain.registration.queue.dto

data class RegistrationQueuePayload(
    val userId: Long,
    val courseId: Long,
    val snapZipCode: String,
    val snapAddress: String,
    val snapDetail: String?,
    val tSize: String,
    val agreedTerms: Boolean
)
