package com.rungo.api.domain.notification.event

@JvmRecord // 기존 자바 코드에서 event.email()로 접근 가능
data class RegistrationCompletedEvent(
    val email: String,
    val marathonTitle: String,
    val courseName: String
)