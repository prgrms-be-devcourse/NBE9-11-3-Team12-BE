package com.rungo.api.domain.registration.queue.dto

import com.rungo.api.domain.registration.dto.CreateRegistrationRes

data class RegistrationQueueResult(
    val success: Boolean,
    val response: CreateRegistrationRes? = null,
    val errorCode: String? = null
)
