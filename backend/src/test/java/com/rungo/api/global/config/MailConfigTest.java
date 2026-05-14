package com.rungo.api.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MailConfigTest {

    // application.yaml 세팅 값 들어오는지 확인
    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Test
    @DisplayName("환경변수(.env)의 SMTP 계정 정보가 정상적으로 주입된다")
    void smtp_env_injection_test() {

        assertThat(username).isNotNull().isNotEmpty();
        assertThat(password).isNotNull().isNotEmpty();

        // ${MAIL_USERNAME} 문자열 치환되었는지 확인
        assertThat(username).doesNotContain("${MAIL_USERNAME}");
        assertThat(password).doesNotContain("${MAIL_PASSWORD}");

        // 확인용 로그 (비밀번호는 보안상 길이만큼 *로 마스킹)
        System.out.println("=====================================");
        System.out.println("주입된 이메일(username): " + username);
        System.out.println("주입된 비밀번호(password): " + "*".repeat(password.length()));
        System.out.println("=====================================");
    }
}