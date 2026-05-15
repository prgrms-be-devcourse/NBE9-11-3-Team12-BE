package com.rungo.api.domain.notification.event

@JvmRecord // 기존 자바 코드에서 event.marathonTitle()로 접근 가능
data class MarathonCanceledEvent(
    val marathonTitle: String,
    val participantEmails: List<String>
)