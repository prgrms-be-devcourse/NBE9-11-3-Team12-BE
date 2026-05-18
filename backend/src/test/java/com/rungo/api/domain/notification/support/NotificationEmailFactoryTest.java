package com.rungo.api.domain.notification.support;

import com.rungo.api.global.infrastructure.mail.EmailMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationEmailFactoryTest {

    private final NotificationEmailFactory factory = new NotificationEmailFactory();

    @Test
    @DisplayName("접수 완료 메일 메시지를 생성한다")
    void registrationCompleted_test() {
        EmailMessage message = factory.registrationCompleted("user@test.com", "서울 마라톤", "10K");

        assertEquals("user@test.com", message.to());
        assertTrue(message.subject().contains("참가 접수 완료"));
        assertTrue(message.body().contains("서울 마라톤"));
        assertTrue(message.body().contains("10K"));
    }

    @Test
    @DisplayName("대회 취소 메일 메시지를 생성한다")
    void marathonCanceled_test() {
        EmailMessage message = factory.marathonCanceled("user@test.com", "서울 마라톤");

        assertEquals("user@test.com", message.to());
        assertTrue(message.subject().contains("대회 취소"));
        assertTrue(message.body().contains("서울 마라톤"));
    }

    @Test
    @DisplayName("결제 완료 메일 메시지를 생성한다")
    void paymentCompleted_test() {
        EmailMessage message = factory.paymentCompleted(
                "user@test.com",
                "서울 마라톤",
                "10K",
                BigDecimal.valueOf(60000)
        );

        assertEquals("user@test.com", message.to());
        assertTrue(message.subject().contains("결제 완료"));
        assertTrue(message.body().contains("60000"));
    }

    @Test
    @DisplayName("환불 완료 메일 메시지를 생성한다")
    void refundCompleted_test() {
        EmailMessage message = factory.refundCompleted(
                "user@test.com",
                "서울 마라톤",
                BigDecimal.valueOf(60000)
        );

        assertEquals("user@test.com", message.to());
        assertTrue(message.subject().contains("환불 완료"));
        assertTrue(message.body().contains("60000"));
    }
}