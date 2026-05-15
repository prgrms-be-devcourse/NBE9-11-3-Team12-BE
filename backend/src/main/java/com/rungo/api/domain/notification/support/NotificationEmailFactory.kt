package com.rungo.api.domain.notification.support

import com.rungo.api.global.infrastructure.mail.EmailMessage
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class NotificationEmailFactory {

    fun registrationCompleted(email: String, marathonTitle: String, courseName: String): EmailMessage {
        val subject = "[Rungo] $marathonTitle 참가 접수 완료 안내"
        val body = """
            안녕하세요!

            $marathonTitle 대회의 [$courseName] 코스 접수가 정상적으로 완료되었습니다.
            마이페이지에서 접수 내역을 확인하실 수 있습니다.
        """.trimIndent()

        return EmailMessage(email, subject, body)
    }

    fun marathonCanceled(email: String, marathonTitle: String): EmailMessage {
        val subject = "[Rungo] $marathonTitle 대회 취소 안내"
        val body = """
            안녕하세요!

            주최측 사정으로 인해 $marathonTitle 대회가 취소되었습니다.
            자세한 사항은 홈페이지를 참고 바랍니다.
        """.trimIndent()

        return EmailMessage(email, subject, body)
    }

    fun paymentCompleted(email: String, marathonTitle: String, courseName: String, amount: BigDecimal): EmailMessage {
        val subject = "[Rungo] $marathonTitle 결제 완료 안내"
        val body = """
            안녕하세요!

            $marathonTitle 대회의 [$courseName] 코스 결제가 완료되었습니다.
            결제 금액: ${amount}원
        """.trimIndent()

        return EmailMessage(email, subject, body)
    }

    fun refundCompleted(email: String, marathonTitle: String, amount: BigDecimal): EmailMessage {
        val subject = "[Rungo] $marathonTitle 환불 완료 안내"
        val body = """
            안녕하세요!

            $marathonTitle 대회 관련 환불이 완료되었습니다.
            환불 금액: ${amount}원
        """.trimIndent()

        return EmailMessage(email, subject, body)
    }
}