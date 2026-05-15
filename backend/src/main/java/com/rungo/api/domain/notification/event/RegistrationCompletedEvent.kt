package com.rungo.api.domain.notification.event

@JvmRecord
data class RegistrationCompletedEvent(val email: String?, val marathonTitle: String?, val courseName: String?) 