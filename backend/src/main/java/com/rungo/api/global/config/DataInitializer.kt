package com.rungo.api.global.config

import com.rungo.api.domain.auth.entity.UserAuth
import com.rungo.api.domain.auth.repository.UserAuthRepository
import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Profile("!test")
@Configuration
class DataInitializer(
    private val userRepository: UserRepository,
    private val userAuthRepository: UserAuthRepository,
    private val marathonRepository: MarathonRepository,
    private val courseRepository: CourseRepository,
    private val registrationRepository: RegistrationRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    companion object {
        private const val TEST_PASSWORD = "Password123!"
        private const val TEST_USER_COUNT = 100
        private const val CANCEL_TEST_MARATHON_COUNT = 100
        private const val USER_BATCH_SIZE = 100
    }

    @Bean
    fun initTestData() = CommandLineRunner {

        val organizer = initOrganizer()
        initParticipants()
        initPerformanceMarathon(organizer)
        initCancelTestMarathons(organizer)

        println("테스트 유저 / 마라톤 / 코스 / 취소 테스트 데이터 생성 완료")
        println("organizer: organizer@test.com / $TEST_PASSWORD")
        println("participants: user1@test.com ~ user$TEST_USER_COUNT@test.com / $TEST_PASSWORD")
    }

    private fun initOrganizer(): Users =
        userRepository.findByEmail("organizer@test.com")
            ?: run {

                val savedUser = Users.create(
                    email = "organizer@test.com",
                    name = "주최자",
                    phoneNumber = "010-2222-2222",
                    gender = Gender.MALE,
                    birth = LocalDate.of(2000, 1, 1),
                ).apply {
                    promoteToOrganizer()
                }

                val saved = userRepository.save(savedUser)

                userAuthRepository.save(
                    UserAuth.createLocalAuth(
                        saved,
                        passwordEncoder.encode(TEST_PASSWORD),
                    )
                )

                saved
            }

    private fun initParticipants() {

        val existingEmails = userRepository
            .findAllByEmailStartingWith("user")
            .map { it.email }
            .toHashSet()

        val batch = mutableListOf<Users>()

        for (i in 1..TEST_USER_COUNT) {

            val email = "user$i@test.com"
            if (email in existingEmails) continue

            batch += Users.create(
                email = email,
                name = "참가자$i",
                phoneNumber = String.format(
                    "010-%04d-%04d",
                    i / 10000,
                    i % 10000,
                ),
                gender = Gender.MALE,
                birth = LocalDate.of(2000, 1, 1),
            )

            if (batch.size == USER_BATCH_SIZE) {

                saveUsersWithLocalAuth(batch)
                println("테스트 유저 저장 진행중: ${i}명")
                batch.clear()
            }
        }

        if (batch.isNotEmpty()) {
            saveUsersWithLocalAuth(batch)
        }

        println("테스트 유저 저장 완료")
    }

    private fun saveUsersWithLocalAuth(usersBatch: List<Users>) {

        val savedUsers = userRepository.saveAll(usersBatch)
        val authBatch = savedUsers.map { user ->
            UserAuth.createLocalAuth(
                user,
                passwordEncoder.encode(TEST_PASSWORD),
            )
        }
        userAuthRepository.saveAll(authBatch)
    }

    private fun initPerformanceMarathon(organizer: Users) {

        val exists = marathonRepository
            .findAllByTitleStartingWith("테스트용 마라톤")
            .any { it.title == "테스트용 마라톤" }

        if (exists) return

        val marathon = Marathon.create(
            organizer = organizer,
            title = "테스트용 마라톤",
            region = "서울",
            detailedAddress = "성동구",
            eventDate = LocalDate.now().plusDays(30),
            posterImageUrl = "poster.png",
            registrationStartAt = LocalDateTime.now().minusDays(1),
            registrationEndAt = LocalDateTime.now().plusDays(10),
        )

        val savedMarathon = marathonRepository.save(marathon)

        val course = Course.create(
            courseType = "10K",
            price = BigDecimal.valueOf(30000),
            capacity = 40000,
            currentCount = 0,
        )

        savedMarathon.addCourse(course)

        courseRepository.save(course)
    }

    private fun initCancelTestMarathons(organizer: Users) {

        val existingTitles = marathonRepository
            .findAllByTitleStartingWith("취소테스트 마라톤")
            .map { it.title }
            .toHashSet()

        val marathonsToSave = mutableListOf<Marathon>()

        for (i in 1..CANCEL_TEST_MARATHON_COUNT) {

            val marathonTitle = "취소테스트 마라톤 $i"
            if (marathonTitle in existingTitles) continue

            marathonsToSave += Marathon.create(
                organizer = organizer,
                title = marathonTitle,
                region = "서울",
                detailedAddress = "성동구",
                eventDate = LocalDate.now().plusDays(30),
                posterImageUrl = "poster.png",
                registrationStartAt = LocalDateTime.now().minusDays(1),
                registrationEndAt = LocalDateTime.now().plusDays(10),
            )
        }

        if (marathonsToSave.isEmpty()) return

        val savedMarathons = marathonRepository.saveAll(marathonsToSave)
        val courses = savedMarathons.map { marathon ->

            Course.create(
                courseType = "10K",
                price = BigDecimal.valueOf(30000),
                capacity = 40000,
                currentCount = 0,
            ).also {
                marathon.addCourse(it)
            }
        }

        courseRepository.saveAll(courses)

        val registrations = savedMarathons.mapIndexed { index, marathon ->
            val participantIndex = index + 1
            val participant = userRepository
                .findByEmail("user$participantIndex@test.com")
                ?: throw IllegalStateException(
                        "취소 테스트용 참가자 없음: user$participantIndex@test.com"
                    )

            val course = marathon.courses[0]

            Registration.create(
                participant,
                course,
                marathon,
                "12345",
                "서울시 강남구",
                "101동",
                "L",
                true,
            )
        }
        registrationRepository.saveAll(registrations)
    }
}