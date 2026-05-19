package com.rungo.api.global.infrastructure.mail

data class EmailMessage(
    val to: String,
    val subject: String,
    val body: String
)