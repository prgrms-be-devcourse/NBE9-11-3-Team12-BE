package com.rungo.api.domain.notification.event

data class RegistrationCompletedEvent(
    val email: String,
    val marathonTitle: String,
    val courseName: String
)