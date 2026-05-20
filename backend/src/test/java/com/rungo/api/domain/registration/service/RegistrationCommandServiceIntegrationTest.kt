package com.rungo.api.domain.registration.service

import com.rungo.api.domain.auth.repository.UserAuthRepository
import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.registration.dto.CreateRegistrationReq
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.repository.UserRepository
import com.rungo.api.global.infrastructure.mail.EmailMessage
import com.rungo.api.global.infrastructure.mail.EmailService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
class RegistrationCommandServiceIntegrationTest {

    @Autowired
    private lateinit var registrationService: RegistrationService

    @Autowired
    private lateinit var registrationRepository: RegistrationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userAuthRepository: UserAuthRepository

    @Autowired
    private lateinit var marathonRepository: MarathonRepository

    @Autowired
    private lateinit var courseRepository: CourseRepository

    @MockitoBean
    private lateinit var emailService: EmailService

    @AfterEach
    fun tearDown() {
        registrationRepository.deleteAllInBatch()
        courseRepository.deleteAllInBatch()
        marathonRepository.deleteAllInBatch()
        userAuthRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    @DisplayName("이메일 발송 실패가 발생해도 참가 접수 데이터는 정상 저장된다")
    fun email_exception_isolation_test() {
        doThrow(RuntimeException("SMTP 서버 강제 다운"))
            .`when`(emailService)
            .send(anyEmailMessage())

        val organizer = saveOrganizer("organizer@test.com")
        val participant = saveParticipant("participant@test.com")
        val course = saveCourseWithMarathon(organizer)

        val result = registrationService.create(participant.id, createRegistrationRequest(course.id))

        assertThat(result.registrationId).isNotNull()
        assertThat(result.marathonTitle).isEqualTo("서울 마라톤")
        assertThat(result.courseType).isEqualTo("10K")
        assertThat(registrationRepository.findById(result.registrationId)).isPresent
        assertThat(findCourse(course.id).currentCount).isEqualTo(1)

        verify(emailService, timeout(2000).atLeastOnce())
            .send(anyEmailMessage())
    }

    @Test
    @DisplayName("참가 접수 성공 시 이메일이 비동기로 발송되고 접수 데이터가 저장된다")
    fun registration_success_email_send_test() {
        val organizer = saveOrganizer("organizer-success@test.com")
        val participant = saveParticipant("participant-success@test.com")
        val course = saveCourseWithMarathon(organizer)

        val result = registrationService.create(participant.id, createRegistrationRequest(course.id))

        assertThat(result.registrationId).isNotNull()
        assertThat(registrationRepository.findById(result.registrationId)).isPresent
        assertThat(findCourse(course.id).currentCount).isEqualTo(1)

        verify(emailService, timeout(2000).times(1))
            .send(anyEmailMessage())
    }

    @Test
    @DisplayName("접수 생성 성공 시 Registration 저장과 currentCount 증가가 DB에 반영된다")
    fun create_success_persists_registration_and_current_count() {
        val organizer = saveOrganizer("organizer-db@test.com")
        val participant = saveParticipant("participant-db@test.com")
        val marathon = saveMarathon(organizer, "서울 마라톤")
        val course = saveCourse(marathon, capacity = 10, currentCount = 0)

        registrationService.create(participant.id, createRegistrationRequest(course.id))

        assertThat(registrationRepository.count()).isEqualTo(1)
        assertThat(findCourse(course.id).currentCount).isEqualTo(1)
    }

    @Test
    @DisplayName("접수 취소 성공 시 Registration 삭제와 currentCount 감소가 DB에 반영된다")
    fun cancel_success_deletes_registration_and_decreases_current_count() {
        val organizer = saveOrganizer("organizer-cancel@test.com")
        val participant = saveParticipant("participant-cancel@test.com")
        val marathon = saveMarathon(organizer, "서울 마라톤")
        val course = saveCourse(marathon, capacity = 10, currentCount = 1)
        val registration = saveRegistration(participant, course, marathon)

        registrationService.cancel(participant.id, registration.id)

        assertThat(registrationRepository.count()).isZero()
        assertThat(findCourse(course.id).currentCount).isZero()
    }

    @Test
    @DisplayName("동일 사용자가 같은 마라톤에 다시 신청하면 유니크 제약으로 실패한다")
    fun create_fail_duplicate_registration() {
        val organizer = saveOrganizer("organizer-duplicate@test.com")
        val participant = saveParticipant("participant-duplicate@test.com")
        val marathon = saveMarathon(organizer, "서울 마라톤")
        val course = saveCourse(marathon, capacity = 10, currentCount = 0)
        val request = createRegistrationRequest(course.id)

        registrationService.create(participant.id, request)

        val exception = assertThrows<DataIntegrityViolationException> {
            registrationService.create(participant.id, request)
        }

        assertThat(registrationRepository.count()).isEqualTo(1)
        assertThat(findCourse(course.id).currentCount).isEqualTo(1)
        assertThat(containsConstraintName(exception, "uk_registration_user_marathon")).isTrue()
    }

    private fun saveOrganizer(email: String): Users =
        Users.create(
            email = email,
            name = "주최자",
            phoneNumber = "010-1111-1111",
            gender = Gender.MALE,
            birth = LocalDate.of(1990, 1, 1)
        ).apply {
            promoteToOrganizer()
        }.let(userRepository::save)

    private fun saveParticipant(email: String): Users =
        Users.create(
            email = email,
            name = "참가자",
            phoneNumber = "010-2222-2222",
            gender = Gender.MALE,
            birth = LocalDate.of(2000, 1, 1)
        ).let(userRepository::save)

    private fun saveCourseWithMarathon(organizer: Users): Course =
        saveCourse(saveMarathon(organizer, "서울 마라톤"), capacity = 100, currentCount = 0)

    private fun createRegistrationRequest(courseId: Long): CreateRegistrationReq =
        CreateRegistrationReq(
            courseId = courseId,
            snapZipCode = "12345",
            snapAddress = "서울시 강남구",
            snapDetail = "101동",
            tSize = "L",
            agreedTerms = true
        )

    private fun saveMarathon(organizer: Users, title: String): Marathon =
        Marathon.create(
            organizer = organizer,
            title = title,
            region = "서울",
            detailedAddress = "성동구",
            eventDate = LocalDate.now().plusDays(10),
            posterImageUrl = "poster.png",
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(5)
        ).let(marathonRepository::saveAndFlush)

    private fun saveCourse(marathon: Marathon, capacity: Int, currentCount: Int): Course {
        val course = Course.create(
            courseType = "10K",
            price = BigDecimal.ZERO,
            capacity = capacity,
            currentCount = currentCount
        )

        marathon.addCourse(course)
        marathonRepository.saveAndFlush(marathon)

        return courseRepository.findAllByMarathon_IdOrderByIdAsc(marathon.id).first()
    }

    private fun saveRegistration(user: Users, course: Course, marathon: Marathon): Registration =
        Registration.createCompleted(
            user = user,
            course = course,
            marathon = marathon,
            snapZipCode = "12345",
            snapAddress = "서울시 강남구",
            snapDetail = "101동",
            tSize = "L",
            agreedTerms = true
        ).let(registrationRepository::saveAndFlush)

    private fun findCourse(courseId: Long): Course =
        courseRepository.findByIdOrNull(courseId)
            ?: error("Course not found: $courseId")

    private fun containsConstraintName(throwable: Throwable?, constraintName: String): Boolean =
        generateSequence(throwable) { it.cause }
            .any { it.message?.contains(constraintName) == true }

    private fun anyEmailMessage(): EmailMessage {
        any(EmailMessage::class.java)
        return EmailMessage("", "", "")
    }
}
