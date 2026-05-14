package com.rungo.api.domain.notification.listener;

import com.rungo.api.domain.notification.event.MarathonCanceledEvent;
import com.rungo.api.domain.notification.event.RegistrationCompletedEvent;
import com.rungo.api.domain.notification.support.NotificationEmailFactory;
import com.rungo.api.global.infrastructure.mail.EmailMessage;
import com.rungo.api.global.infrastructure.mail.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationEmailFactory notificationEmailFactory;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Test
    @DisplayName("접수 완료 이벤트 발생 시 이메일 메시지를 생성하고 발송한다")
    void handleRegistrationCompleted_test() {
        RegistrationCompletedEvent event =
                new RegistrationCompletedEvent("test@gmail.com", "서울 마라톤", "10km");

        EmailMessage message = new EmailMessage(
                "test@gmail.com",
                "[Rungo] 서울 마라톤 참가 접수 완료 안내",
                "body"
        );

        given(notificationEmailFactory.registrationCompleted(
                event.email(), event.marathonTitle(), event.courseName())
        ).willReturn(message);

        notificationEventListener.handleRegistrationCompleted(event);

        verify(notificationEmailFactory, times(1))
                .registrationCompleted(event.email(), event.marathonTitle(), event.courseName());
        verify(emailService, times(1)).send(message);
    }

    @Test
    @DisplayName("대회 취소 이벤트 발생 시 참가자 수만큼 이메일 메시지를 생성하고 발송한다")
    void handleMarathonCanceled_test() {
        List<String> emails = List.of("user1@gmail.com", "user2@gmail.com");
        MarathonCanceledEvent event = new MarathonCanceledEvent("서울 마라톤", emails);

        given(notificationEmailFactory.marathonCanceled("user1@gmail.com", "서울 마라톤"))
                .willReturn(new EmailMessage("user1@gmail.com", "subject1", "body1"));
        given(notificationEmailFactory.marathonCanceled("user2@gmail.com", "서울 마라톤"))
                .willReturn(new EmailMessage("user2@gmail.com", "subject2", "body2"));

        notificationEventListener.handleMarathonCanceled(event);

        verify(notificationEmailFactory, times(1)).marathonCanceled("user1@gmail.com", "서울 마라톤");
        verify(notificationEmailFactory, times(1)).marathonCanceled("user2@gmail.com", "서울 마라톤");
        verify(emailService, times(2)).send(org.mockito.ArgumentMatchers.any(EmailMessage.class));
    }
}