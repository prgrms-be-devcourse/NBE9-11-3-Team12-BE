package com.rungo.api.global.infrastructure.mail

@JvmRecord
data class EmailMessage(
    val to: String,
    val subject: String,
    val body: String
)