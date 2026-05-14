package com.rungo.api.domain.notification.event;

public record RegistrationCompletedEvent(String email, String marathonTitle, String courseName) {}