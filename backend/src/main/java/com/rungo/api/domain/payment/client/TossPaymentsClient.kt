package com.rungo.api.domain.payment.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.rungo.api.domain.payment.dto.TossCancelReq
import com.rungo.api.domain.payment.dto.TossConfirmReq
import com.rungo.api.domain.payment.dto.TossErrorRes
import com.rungo.api.domain.payment.dto.TossPaymentRes
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*

@Component
class TossPaymentsClient(
    @Value("\${toss-payments.base-url:https://api.tosspayments.com}")
    private val baseUrl: String,

    @Value("\${toss-payments.secret-key:}")
    private val secretKey: String,

    @Value("\${toss-payments.connect-timeout-ms:3000}")
    private val connectTimeoutMs: Long,

    @Value("\${toss-payments.read-timeout-ms:10000}")
    private val readTimeoutMs: Long,

    private val objectMapper: ObjectMapper,
) {
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(tossRequestFactory())
        .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuthorizationValue())
        .build()

    // 토스 결제 승인 API 호출
    fun confirm(
        paymentKey: String,
        orderId: String,
        amount: Long,
    ): TossPaymentRes =
        restClient.post()
            .uri("/v1/payments/confirm")
            .body(TossConfirmReq(paymentKey, orderId, amount))
            .retrieve()
            .onStatus(HttpStatusCode::isError, tossErrorHandler())
            .body(TossPaymentRes::class.java)
            ?: throw TossPaymentsException(
                code = "EMPTY_RESPONSE",
                message = "토스페이먼츠 응답이 비어 있습니다.",
            )

    // 토스 결제 취소 API 호출
    fun cancel(
        paymentKey: String,
        cancelReason: String,
        cancelAmount: Long? = null,
        idempotencyKey: String,
    ): TossPaymentRes =
        restClient.post()
            .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
            .header("Idempotency-Key", idempotencyKey)
            .body(TossCancelReq(cancelReason, cancelAmount))
            .retrieve()
            .onStatus(HttpStatusCode::isError, tossErrorHandler())
            .body(TossPaymentRes::class.java)
            ?: throw TossPaymentsException(
                code = "EMPTY_RESPONSE",
                message = "토스페이먼츠 응답이 비어 있습니다.",
            )

    // 토스 에러 응답을 내부 예외로 변환
    private fun tossErrorHandler(): ErrorHandler = ErrorHandler { _, response ->
        val rawBody = response.body
            .readAllBytes()
            .toString(StandardCharsets.UTF_8)

        val error = runCatching {
            objectMapper.readValue(rawBody, TossErrorRes::class.java)
        }.getOrNull()

        throw TossPaymentsException(
            code = error?.code ?: response.statusCode.toString(),
            message = error?.message
                ?: rawBody.ifBlank { "토스페이먼츠 API 요청에 실패했습니다." },
        )
    }

    // 토스 API 요청 타임아웃 설정
    private fun tossRequestFactory(): SimpleClientHttpRequestFactory =
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
            setReadTimeout(Duration.ofMillis(readTimeoutMs))
        }

    // 토스 시크릿 키 기반 Basic 인증 헤더 생성
    private fun basicAuthorizationValue(): String {
        val credential = "$secretKey:"
        val encoded = Base64.getEncoder()
            .encodeToString(credential.toByteArray(StandardCharsets.UTF_8))

        return "Basic $encoded"
    }
}