package com.rungo.api.global.config

import com.rungo.api.domain.auth.entity.UserAuth
import com.rungo.api.domain.auth.repository.UserAuthRepository
import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.payment.entity.Payment
import com.rungo.api.domain.payment.enumtype.PaymentStatus
import com.rungo.api.domain.payment.repository.PaymentRepository
import com.rungo.api.domain.payment.support.OrderIdGenerator
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import com.rungo.api.domain.registration.enumtype.RegistrationStatus
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.enumtype.Provider
import com.rungo.api.domain.users.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
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
    private val registrationCancelHistoryRepository: RegistrationCancelHistoryRepository,
    private val paymentRepository: PaymentRepository,
    private val orderIdGenerator: OrderIdGenerator,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${payment.expire-minutes:30}")
    private val paymentExpireMinutes: Long,
) {

    companion object {
        private const val TEST_PASSWORD = "Password123!"
        private const val TEST_USER_COUNT = 100
        private const val CANCEL_TEST_MARATHON_COUNT = 100
        private const val USER_BATCH_SIZE = 100
    }

    @Bean
    fun initTestData() = CommandLineRunner {
        initAdmin()
        val organizer = initOrganizer()
        initParticipants()
        initPerformanceMarathon(organizer)
        initCancelTestMarathons(organizer)
        initCleanupTestData(organizer)

        println("테스트 유저 / 마라톤 / 코스 / 취소 테스트 데이터 생성 완료")
        println("admin: admin@test.com / $TEST_PASSWORD")
        println("organizer: organizer@test.com / $TEST_PASSWORD")
        println("participants: user1@test.com ~ user$TEST_USER_COUNT@test.com / $TEST_PASSWORD")
    }

    private fun initAdmin() {
        val existing = userRepository.findByEmail("admin@test.com")
        if (existing != null) {
            existing.promoteToAdmin()
            userRepository.save(existing)
            ensureLocalAuth(existing)
            return
        }

        val admin = Users.create(
            email = "admin@test.com",
            name = "관리자",
            phoneNumber = "010-0000-0000",
            gender = Gender.MALE,
            birth = LocalDate.of(2000, 1, 1),
        ).apply {
            promoteToAdmin()
        }

        val saved = userRepository.save(admin)
        ensureLocalAuth(saved)
    }

    private fun initOrganizer(): Users {
        val existing = userRepository.findByEmail("organizer@test.com")
        if (existing != null) {
            existing.promoteToOrganizer()
            val saved = userRepository.save(existing)
            ensureLocalAuth(saved)
            return saved
        }

        val organizer = Users.create(
            email = "organizer@test.com",
            name = "주최자",
            phoneNumber = "010-2222-2222",
            gender = Gender.MALE,
            birth = LocalDate.of(2000, 1, 1),
        ).apply {
            promoteToOrganizer()
        }

        val saved = userRepository.save(organizer)
        ensureLocalAuth(saved)

        return saved
    }

    private fun initParticipants() {
        val existingUsers = userRepository
            .findAllByEmailStartingWith("user")
            .associateBy { it.email }

        val batch = mutableListOf<Users>()

        for (i in 1..TEST_USER_COUNT) {
            val email = "user$i@test.com"

            val existing = existingUsers[email]
            if (existing != null) {
                ensureLocalAuth(existing)
                continue
            }

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

        if (marathonsToSave.isEmpty()) {
            ensurePaymentsForExistingCancelTestMarathons()
            return
        }

        val savedMarathons = marathonRepository.saveAll(marathonsToSave)
        val courses = savedMarathons.map { marathon ->
            Course.create(
                courseType = "10K",
                price = BigDecimal.valueOf(100),
                capacity = 40000,
                currentCount = 1,
            ).also { course ->
                marathon.addCourse(course)
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

            Registration.createCompleted(
                user = participant,
                course = course,
                marathon = marathon,
                snapZipCode = "12345",
                snapAddress = "서울시 강남구",
                snapDetail = "101동",
                tSize = "L",
                agreedTerms = true,
            )
        }

        val savedRegistrations = registrationRepository.saveAll(registrations)
        savePaymentsIfMissing(savedRegistrations) { PaymentStatus.DONE }
    }

    private fun initCleanupTestData(organizer: Users) {
        val title = "초기화 테스트용 마라톤"
        val exists = marathonRepository.findAllByTitleStartingWith(title).any { it.title == title }
        if (exists) {
            ensurePaymentsForExistingCleanupTestData(title)
            return
        }

        val marathon = Marathon.create(
            organizer = organizer,
            title = title,
            region = "서울",
            detailedAddress = "성동구",
            eventDate = LocalDate.now().minusYears(5).minusDays(1),
            posterImageUrl = "poster.png",
            registrationStartAt = LocalDateTime.now().minusYears(5).minusMonths(3),
            registrationEndAt = LocalDateTime.now().minusYears(5).minusMonths(1),
        )
        val savedMarathon = marathonRepository.save(marathon)

        val course = Course.create(
            courseType = "10K",
            price = BigDecimal.valueOf(30000),
            capacity = 1000,
            currentCount = 3,
        )
        savedMarathon.addCourse(course)
        val savedCourse = courseRepository.save(course)

        val activeRegistrations = (4..6).map { i ->
            val user = userRepository.findByEmail("user$i@test.com")
                ?: throw IllegalStateException("user$i@test.com not found")

            Registration.createCompleted(
                user = user,
                course = savedCourse,
                marathon = savedMarathon,
                snapZipCode = "12345",
                snapAddress = "서울시 강남구",
                snapDetail = "101동",
                tSize = "M",
                agreedTerms = true,
            )
        }
        val savedActiveRegistrations = registrationRepository.saveAll(activeRegistrations)
        savePaymentsIfMissing(savedActiveRegistrations) { PaymentStatus.DONE }

        val cancelRegistrations = (1..3).map { i ->
            val user = userRepository.findByEmail("user$i@test.com")
                ?: throw IllegalStateException("user$i@test.com not found")

            Registration.createPendingPayment(
                user = user,
                course = savedCourse,
                marathon = savedMarathon,
                snapZipCode = "12345",
                snapAddress = "서울시 강남구",
                snapDetail = "101동",
                tSize = "L",
                agreedTerms = true,
            )
        }
        val savedCancelRegistrations = registrationRepository.saveAll(cancelRegistrations)
        savePaymentsIfMissing(savedCancelRegistrations) { PaymentStatus.CANCELED }
        registrationCancelHistoryRepository.saveAll(
            savedCancelRegistrations.map { RegistrationCancelHistory.create(it) }
        )
        registrationRepository.deleteAll(savedCancelRegistrations)

        println("초기화 테스트 데이터 생성 완료 (5년 전 마라톤 / 접수 3건 / 취소 내역 3건)")
    }

    private fun ensurePaymentsForExistingCancelTestMarathons() {
        val marathons = marathonRepository.findAllByTitleStartingWith("취소테스트 마라톤")

        marathons.forEach { marathon ->
            val registrations = registrationRepository.findAllByMarathon_IdOrderByAppliedAtDesc(marathon.id)
            savePaymentsIfMissing(registrations, ::resolvePaymentStatusForExistingRegistration)
        }
    }

    private fun ensurePaymentsForExistingCleanupTestData(title: String) {
        val marathon = marathonRepository.findAllByTitleStartingWith(title)
            .firstOrNull { it.title == title }
            ?: return

        val registrations = registrationRepository.findAllByMarathon_IdOrderByAppliedAtDesc(marathon.id)
        savePaymentsIfMissing(registrations, ::resolvePaymentStatusForExistingRegistration)
    }

    private fun resolvePaymentStatusForExistingRegistration(registration: Registration): PaymentStatus =
        when (registration.status) {
            RegistrationStatus.COMPLETED -> PaymentStatus.DONE
            RegistrationStatus.PENDING_PAYMENT -> PaymentStatus.READY
            RegistrationStatus.CANCELED -> PaymentStatus.CANCELED
        }

    private fun savePaymentsIfMissing(
        registrations: List<Registration>,
        statusResolver: (Registration) -> PaymentStatus,
    ) {
        val payments = registrations.mapNotNull { registration ->
            if (registration.course.price.signum() == 0) return@mapNotNull null
            if (paymentRepository.findByOriginalRegistrationId(registration.id) != null) return@mapNotNull null

            createPayment(registration, statusResolver(registration))
        }

        if (payments.isNotEmpty()) {
            paymentRepository.saveAll(payments)
        }
    }

    private fun createPayment(
        registration: Registration,
        status: PaymentStatus,
    ): Payment {
        val now = LocalDateTime.now()
        val amount = registration.course.price.toBigIntegerExact().longValueExact()

        val payment = Payment.createReady(
            registration = registration,
            orderId = orderIdGenerator.generate(registration.id, now),
            amount = amount,
            now = now,
            expireMinutes = paymentExpireMinutes,
        )

        when (status) {
            PaymentStatus.READY -> Unit
            PaymentStatus.DONE -> payment.complete(
                paymentKey = "test-payment-key-${registration.id}",
                method = "카드",
                approvedAt = now,
            )

            PaymentStatus.CANCELED -> payment.cancel()
            else -> throw IllegalArgumentException("지원하지 않는 테스트 결제 상태입니다: $status")
        }

        return payment
    }

    private fun ensureLocalAuth(user: Users) {
        if (userAuthRepository.existsByUserAndProvider(user, Provider.LOCAL)) return

        userAuthRepository.save(
            UserAuth.createLocalAuth(
                user,
                passwordEncoder.encode(TEST_PASSWORD),
            )
        )
    }
}