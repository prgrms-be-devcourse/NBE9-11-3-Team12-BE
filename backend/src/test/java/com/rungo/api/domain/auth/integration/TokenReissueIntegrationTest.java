package com.rungo.api.domain.auth.integration;

import com.rungo.api.domain.auth.service.RefreshTokenService;
import com.rungo.api.global.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class TokenReissueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    @DisplayName("동시에 토큰 재발급 요청이 들어와도 하나만 성공해야 한다")
    void concurrentTokenReissueTest() throws InterruptedException {

        Long userId = 1L;
        String refreshToken = JwtUtil.generateRefreshToken(userId, "test@test.com", jwtSecret);
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        int threadCount = 5; // 동시 요청 수
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<Integer> statusCodes = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    MvcResult result = mockMvc.perform(
                            post("/api/v1/auth/reissue")
                                    .cookie(new MockCookie("refreshToken", refreshToken))
                    ).andReturn();

                    statusCodes.add(result.getResponse().getStatus());

                } catch (Exception e) {
                    statusCodes.add(500);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(); // 5개 모두 준비될 때까지 대기
        startLatch.countDown(); // 동시 요청
        doneLatch.await(); // 모든 스레드 완료될 때까지 대기

        executorService.shutdown();

        long successCount = statusCodes.stream().filter(code -> code == 200).count();
        long failCount = statusCodes.stream()
                .filter(code -> code == 409 || code == 401 || code == 404)
                .count();


//        System.out.println("성공 횟수: " + successCount);
//        System.out.println("실패 횟수: " + failCount);
//        System.out.println("상태코드 목록: " + statusCodes);

        assertThat(successCount).isEqualTo(1); // 1번만 성공해야 한다.
        assertThat(failCount).isEqualTo(threadCount - 1);
    }
}