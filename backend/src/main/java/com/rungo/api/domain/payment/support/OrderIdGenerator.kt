package com.rungo.api.domain.payment.support

import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class OrderIdGenerator {
    fun generate(registrationId: Long, now: LocalDateTime): String {
        // 주문 ID 식별을 위한 날짜 파트 생성
        val datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        // 주문 ID 중복 방지를 위한 랜덤 파트 생성
        val randomPart = UUID.randomUUID().toString().replace("-", "").take(10).uppercase()

        return "REG-$registrationId-$datePart-$randomPart"
    }
}