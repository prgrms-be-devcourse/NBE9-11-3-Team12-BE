package com.rungo.api.domain.notification.event

data class MarathonCanceledEvent(
    val marathonTitle: String,
    val participantEmails: List<String>
)