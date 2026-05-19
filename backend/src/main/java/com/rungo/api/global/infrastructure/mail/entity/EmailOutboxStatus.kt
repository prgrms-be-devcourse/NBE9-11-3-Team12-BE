package com.rungo.api.global.infrastructure.mail.entity

enum class EmailOutboxStatus {
    PENDING,
    SUCCESS,
    FAILED,
    EXHAUSTED
}