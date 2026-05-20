package com.rungo.api.domain.marathon.marathon.service

import com.rungo.api.domain.auth.repository.UserAuthRepository
import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.enumtype.MarathonStatus
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.infrastructure.mail.entity.EmailOutboxStatus
import com.rungo.api.global.infrastructure.mail.repository.EmailOutboxRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
internal class MarathonServiceIntegrationTest {

    @Autowired
    lateinit var marathonService: MarathonService

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userAuthRepository: UserAuthRepository

    @Autowired
    lateinit var marathonRepository: MarathonRepository

    @Autowired
    lateinit var courseRepository: CourseRepository

    @Autowired
    lateinit var registrationRepository: RegistrationRepository

    @Autowired
    lateinit var emailOutboxRepository: EmailOutboxRepository

    @AfterEach
    fun tearDown() {
        emailOutboxRepository.deleteAllInBatch()
        registrationRepository.deleteAllInBatch()
        courseRepository.deleteAllInBatch()
        marathonRepository.deleteAllInBatch()
        userAuthRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("대회 취소 성공 시 참가자 수만큼 Outbox에 이메일 발송 정보가 저장되고 상태가 변경된다")
    fun cancelMarathonSuccessEmailOutboxTest() {
        val organizer = saveOrganizer("organizer@test.com")
        val participant1 = saveParticipant("user1@test.com")
        val participant2 = saveParticipant("user2@test.com")

        val marathon = saveMarathon(organizer)
        val course = saveCourse(marathon)

        saveRegistration(participant1, marathon, course)
        saveRegistration(participant2, marathon, course)

        marathonService.cancelMarathon(organizer.id, marathon.id)

        val savedMarathon =
            marathonRepository.findById(marathon.id)
                .orElseThrow()

        assertThat(savedMarathon.status)
            .isEqualTo(MarathonStatus.CANCELED)

        val outboxes =
            emailOutboxRepository.findAll()
                .filter {
                    it.recipient in listOf(
                        "user1@test.com",
                        "user2@test.com",
                    )
                }
                .sortedBy { it.recipient }

        assertThat(outboxes).hasSize(2)

        assertThat(outboxes[0].recipient)
            .isEqualTo("user1@test.com")
        assertThat(outboxes[0].subject)
            .contains("대회 취소")
        assertThat(outboxes[0].status)
            .isEqualTo(EmailOutboxStatus.PENDING)

        assertThat(outboxes[1].recipient)
            .isEqualTo("user2@test.com")
        assertThat(outboxes[1].subject)
            .contains("대회 취소")
        assertThat(outboxes[1].status)
            .isEqualTo(EmailOutboxStatus.PENDING)

    }

    @Test
    @DisplayName("대회 취소 시 Outbox 저장이 발생해도 상태 변경은 정상 커밋된다")
    fun cancelMarathonOutboxIsolationTest() {
        val organizer = saveOrganizer("organizer-fail@test.com")
        val participant1 = saveParticipant("fail-user1@test.com")
        val participant2 = saveParticipant("fail-user2@test.com")

        val marathon = saveMarathon(organizer)
        val course = saveCourse(marathon)

        saveRegistration(participant1, marathon, course)
        saveRegistration(participant2, marathon, course)

        marathonService.cancelMarathon(organizer.id, marathon.id)

        val savedMarathon =
            marathonRepository.findById(marathon.id)
                .orElseThrow()

        assertThat(savedMarathon.status)
            .isEqualTo(MarathonStatus.CANCELED)

        val outboxes =
            emailOutboxRepository.findAll()
                .filter {
                    it.recipient in listOf(
                        "fail-user1@test.com",
                        "fail-user2@test.com",
                    )
                }

        assertThat(outboxes).hasSize(2)
    }

    private fun saveOrganizer(email: String): Users =
        Users.create(
            email,
            "주최자",
            "010-1111-1111",
            Gender.MALE,
            LocalDate.of(1990, 1, 1),
        ).apply {
            promoteToOrganizer()
        }.let(userRepository::save)

    private fun saveParticipant(email: String): Users =
        userRepository.save(
            Users.create(
                email,
                "참가자",
                "010-2222-2222",
                Gender.MALE,
                LocalDate.of(2000, 1, 1),
            )
        )

    private fun saveMarathon(organizer: Users): Marathon =
        marathonRepository.save(
            Marathon.create(
                organizer,
                "서울 마라톤",
                "서울",
                "성동구",
                LocalDate.now().plusDays(10),
                "poster.png",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(5),
            )
        )

    private fun saveCourse(marathon: Marathon): Course {
        val course = Course.create(
            "10K",
            BigDecimal.valueOf(30000),
            100,
            2,
        )

        marathon.addCourse(course)

        val savedMarathon =
            marathonRepository.save(marathon)

        return savedMarathon.courses[0]
    }

    private fun saveRegistration(
        user: Users,
        marathon: Marathon,
        course: Course,
    ): Registration =
        registrationRepository.save(
            Registration.createCompleted(
                user,
                course,
                marathon,
                "12345",
                "서울시 강남구",
                "101동",
                "L",
                true,
            )
        )
}