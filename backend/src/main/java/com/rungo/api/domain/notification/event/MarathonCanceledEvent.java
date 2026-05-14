package com.rungo.api.domain.notification.event;
import java.util.List;
public record MarathonCanceledEvent(String marathonTitle, List<String> participantEmails) {}