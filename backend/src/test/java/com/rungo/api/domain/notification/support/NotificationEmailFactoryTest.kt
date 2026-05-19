package com.rungo.api.domain.notification.support

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class NotificationEmailFactoryTest {

    private val factory = NotificationEmailFactory()

    @Test
    @DisplayName("접수 완료 메일 메시지를 생성한다")
    fun registrationCompleted_test() {
        val message = factory.registrationCompleted("user@test.com", "서울 마라톤", "10K")

        assertEquals("user@test.com", message.to)
        assertTrue(message.subject.contains("참가 접수 완료"))
        assertTrue(message.body.contains("서울 마라톤"))
        assertTrue(message.body.contains("10K"))
    }

    @Test
    @DisplayName("대회 취소 메일 메시지를 생성한다")
    fun marathonCanceled_test() {
        val message = factory.marathonCanceled("user@test.com", "서울 마라톤")

        assertEquals("user@test.com", message.to)
        assertTrue(message.subject.contains("대회 취소"))
        assertTrue(message.body.contains("서울 마라톤"))
    }

    @Test
    @DisplayName("결제 완료 메일 메시지를 생성한다")
    fun paymentCompleted_test() {
        val message = factory.paymentCompleted(
            "user@test.com",
            "서울 마라톤",
            "10K",
            BigDecimal.valueOf(60000)
        )

        assertEquals("user@test.com", message.to)
        assertTrue(message.subject.contains("결제 완료"))
        assertTrue(message.body.contains("60000"))
    }

    @Test
    @DisplayName("환불 완료 메일 메시지를 생성한다")
    fun refundCompleted_test() {
        val message = factory.refundCompleted(
            "user@test.com",
            "서울 마라톤",
            BigDecimal.valueOf(60000)
        )

        assertEquals("user@test.com", message.to)
        assertTrue(message.subject.contains("환불 완료"))
        assertTrue(message.body.contains("60000"))
    }
}