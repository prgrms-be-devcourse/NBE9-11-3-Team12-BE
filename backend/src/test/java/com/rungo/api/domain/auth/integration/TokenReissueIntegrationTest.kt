package com.rungo.api.domain.auth.integration

import com.rungo.api.domain.auth.service.RefreshTokenService
import com.rungo.api.global.util.JwtUtil
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockCookie
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
@AutoConfigureMockMvc
internal class TokenReissueIntegrationTest {
    @Autowired
    private val mockMvc: MockMvc? = null

    @Autowired
    private val refreshTokenService: RefreshTokenService? = null

    @Value("\${jwt.secret}")
    private val jwtSecret: String? = null

    @Test
    @DisplayName("동시에 토큰 재발급 요청이 들어와도 하나만 성공해야 한다")
    @Throws(InterruptedException::class)
    fun concurrentTokenReissueTest() {
        val userId = 1L
        val refreshToken = JwtUtil.generateRefreshToken(userId, "test@test.com", jwtSecret!!)
        refreshTokenService!!.saveRefreshToken(userId, refreshToken)

        val threadCount = 5 // 동시 요청 수
        val executorService = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        val statusCodes: MutableList<Int?> = CopyOnWriteArrayList<Int?>()

        for (i in 0..<threadCount) {
            executorService.submit(Runnable {
                try {
                    readyLatch.countDown()
                    startLatch.await()

                    val result = mockMvc!!.perform(
                        MockMvcRequestBuilders.post("/api/v1/auth/reissue")
                            .cookie(MockCookie("refreshToken", refreshToken))
                    ).andReturn()

                    statusCodes.add(result.getResponse().getStatus())
                } catch (e: Exception) {
                    statusCodes.add(500)
                } finally {
                    doneLatch.countDown()
                }
            })
        }

        readyLatch.await() // 5개 모두 준비될 때까지 대기
        startLatch.countDown() // 동시 요청
        doneLatch.await() // 모든 스레드 완료될 때까지 대기

        executorService.shutdown()

        val successCount = statusCodes.stream().filter { code: Int? -> code == 200 }.count()
        val failCount = statusCodes.stream()
            .filter { code: Int? -> code == 409 || code == 401 || code == 404 }
            .count()


        //        System.out.println("성공 횟수: " + successCount);
//        System.out.println("실패 횟수: " + failCount);
//        System.out.println("상태코드 목록: " + statusCodes);
        AssertionsForClassTypes.assertThat(successCount).isEqualTo(1) // 1번만 성공해야 한다.
        AssertionsForClassTypes.assertThat(failCount).isEqualTo((threadCount - 1).toLong())
    }
}