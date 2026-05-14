package com.rungo.api.domain.notification.support;

import com.rungo.api.global.infrastructure.mail.EmailMessage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NotificationEmailFactory {

    public EmailMessage registrationCompleted(String email, String marathonTitle, String courseName) {
        String subject = "[Rungo] " + marathonTitle + " 참가 접수 완료 안내";
        String body = String.format(
                """
                안녕하세요!

                %s 대회의 [%s] 코스 접수가 정상적으로 완료되었습니다.
                마이페이지에서 접수 내역을 확인하실 수 있습니다.
                """,
                marathonTitle,
                courseName
        );

        return new EmailMessage(email, subject, body);
    }

    public EmailMessage marathonCanceled(String email, String marathonTitle) {
        String subject = "[Rungo] " + marathonTitle + " 대회 취소 안내";
        String body = String.format(
                """
                안녕하세요!

                주최측 사정으로 인해 %s 대회가 취소되었습니다.
                자세한 사항은 홈페이지를 참고 바랍니다.
                """,
                marathonTitle
        );

        return new EmailMessage(email, subject, body);
    }

    public EmailMessage paymentCompleted(String email, String marathonTitle, String courseName, BigDecimal amount) {
        String subject = "[Rungo] " + marathonTitle + " 결제 완료 안내";
        String body = String.format(
                """
                안녕하세요!

                %s 대회의 [%s] 코스 결제가 완료되었습니다.
                결제 금액: %s원
                """,
                marathonTitle,
                courseName,
                amount
        );

        return new EmailMessage(email, subject, body);
    }

    public EmailMessage refundCompleted(String email, String marathonTitle, BigDecimal amount) {
        String subject = "[Rungo] " + marathonTitle + " 환불 완료 안내";
        String body = String.format(
                """
                안녕하세요!

                %s 대회 관련 환불이 완료되었습니다.
                환불 금액: %s원
                """,
                marathonTitle,
                amount
        );

        return new EmailMessage(email, subject, body);
    }
}