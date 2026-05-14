package com.rungo.api.global.infrastructure.mail;

public record EmailMessage(
        String to,
        String subject,
        String body
) {
}