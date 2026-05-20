package com.rungo.api.global.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class MailConfigTest {

    private val contextRunner = ApplicationContextRunner()
        .withPropertyValues(
            "spring.mail.username=test@example.com",
            "spring.mail.password=test-password",
        )

    @Test
    @DisplayName("테스트용 SMTP 계정 정보가 정상적으로 주입된다")
    fun smtp_property_injection_test() {
        contextRunner.run { context ->
            val username = context.environment.getProperty("spring.mail.username")
            val password = context.environment.getProperty("spring.mail.password")

            assertThat(username).isEqualTo("test@example.com")
            assertThat(password).isEqualTo("test-password")

            println("=====================================")
            println("주입된 이메일(username): $username")
            println("주입된 비밀번호(password): ${"*".repeat(password!!.length)}")
            println("=====================================")
        }
    }
}