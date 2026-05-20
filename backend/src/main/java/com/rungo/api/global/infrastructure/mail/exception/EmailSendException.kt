package com.rungo.api.global.infrastructure.mail.exception

class EmailSendException(
    message: String,
    cause: Throwable
) : RuntimeException(message, cause)