package com.rungo.api.domain.registration.service

import com.rungo.api.domain.marathon.course.entity.Course
import com.rungo.api.domain.marathon.course.repository.CourseRepository
import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.registration.dto.MyRegistrationRes
import com.rungo.api.domain.registration.entity.Registration
import com.rungo.api.domain.registration.entity.RegistrationCancelHistory
import com.rungo.api.domain.registration.enumtype.MyRegistrationStatusFilter
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import com.rungo.api.domain.users.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.*
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class RegistrationReadServiceTest {

    @InjectMocks
    private lateinit var registrationService: RegistrationService

    @Mock
    private lateinit var registrationRepository: RegistrationRepository

    @Mock
    private lateinit var registrationCancelHistoryRepository: RegistrationCancelHistoryRepository

    @Mock
    private lateinit var courseRepository: CourseRepository

    @Mock
    private lateinit var marathonRepository: MarathonRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Test
    @DisplayName("내 접수 조회 성공 - ACTIVE 조회 시 appliedAt, id 내림차순으로 정상 접수 목록을 반환한다")
    fun getMyRegistrations_active_success() {
        val userId = 1L
        val pageable = PageRequest.of(0, 20)

        val user = createUser(1L, "홍길동", "010-1111-2222")
        val marathon = createMarathon()
        ReflectionTestUtils.setField(marathon, "id", 10L)

        val course = createCourse(marathon, "10K", 50000, 100, 10)
        ReflectionTestUtils.setField(course, "id", 20L)

        val registration = Registration.create(
            user = user,
            course = course,
            marathon = marathon,
            snapZipCode = "12345",
            snapAddress = "서울시 강남구",
            snapDetail = "101동",
            tSize = "L",
            agreedTerms = true
        )
        ReflectionTestUtils.setField(registration, "id", 30L)
        ReflectionTestUtils.setField(
            registration,
            "appliedAt",
            LocalDateTime.of(2026, 4, 20, 9, 30)
        )

        val page = PageImpl(listOf(registration), PageRequest.of(0, 20), 1)

        given(registrationRepository.findByUser_Id(eq(userId), anyPageable()))
            .willReturn(page)

        val result = registrationService.getMyRegistrations(
            userId,
            MyRegistrationStatusFilter.ACTIVE,
            pageable
        )

        val pageableCaptor = ArgumentCaptor.forClass(Pageable::class.java)

        verify(registrationRepository).findByUser_Id(
            eq(userId),
            capturePageable(pageableCaptor)
        )
        verify(registrationCancelHistoryRepository, never()).findByUserId(
            anyLong(),
            anyPageable()
        )

        val captured = pageableCaptor.value
        assertEquals(0, captured.pageNumber)
        assertEquals(20, captured.pageSize)
        assertEquals(Sort.Direction.DESC, captured.sort.getOrderFor("appliedAt")?.direction)
        assertEquals(Sort.Direction.DESC, captured.sort.getOrderFor("id")?.direction)

        assertNotNull(result)
        assertEquals(1, result.content.size)
        assertEquals(0, result.pageRes.page)
        assertEquals(20, result.pageRes.size)
        assertEquals(1L, result.pageRes.totalElements)
        assertEquals(1, result.pageRes.totalPages)

        val item: MyRegistrationRes.Item = result.content[0]
        assertEquals(30L, item.registrationId)
        assertNull(item.historyId)
        assertEquals(10L, item.marathonId)
        assertEquals("서울 마라톤", item.marathonTitle)
        assertEquals(20L, item.courseId)
        assertEquals("10K", item.courseType)
        assertEquals("ACTIVE", item.status)
        assertEquals(BigDecimal("50000"), item.price)
        assertEquals(LocalDate.of(2026, 10, 25), item.eventDate)
        assertEquals("홍길동", item.snapName)
        assertEquals("010-1111-2222", item.snapPhoneNumber)
        assertEquals("12345", item.snapZipCode)
        assertEquals("서울시 강남구", item.snapAddress)
        assertEquals("101동", item.snapDetail)
        assertEquals("L", item.tSize)
        assertEquals(true, item.agreedTerms)
        assertEquals(LocalDateTime.of(2026, 4, 20, 9, 30), item.appliedAt)
        assertNull(item.canceledAt)
    }

    @Test
    @DisplayName("내 접수 조회 성공 - CANCELED 조회 시 canceledAt, id 내림차순으로 취소 접수 목록을 반환한다")
    fun getMyRegistrations_canceled_success() {
        val userId = 1L
        val pageable = PageRequest.of(1, 10)

        val user = createUser(1L, "홍길동", "010-1111-2222")
        val marathon = createMarathon()
        ReflectionTestUtils.setField(marathon, "id", 100L)

        val course = createCourse(marathon, "Half", 70000, 200, 50)
        ReflectionTestUtils.setField(course, "id", 200L)

        val registration = Registration.create(
            user = user,
            course = course,
            marathon = marathon,
            snapZipCode = "54321",
            snapAddress = "서울시 송파구",
            snapDetail = "202동",
            tSize = "M",
            agreedTerms = true
        )
        ReflectionTestUtils.setField(registration, "id", 300L)
        ReflectionTestUtils.setField(
            registration,
            "appliedAt",
            LocalDateTime.of(2026, 4, 1, 8, 0)
        )

        val history = RegistrationCancelHistory.create(registration)
        ReflectionTestUtils.setField(history, "id", 400L)
        ReflectionTestUtils.setField(
            history,
            "canceledAt",
            LocalDateTime.of(2026, 4, 5, 18, 30)
        )

        val page = PageImpl(listOf(history), PageRequest.of(1, 10), 11)

        given(registrationCancelHistoryRepository.findByUserId(eq(userId), anyPageable()))
            .willReturn(page)
        given(marathonRepository.findAllById(setOf(100L)))
            .willReturn(listOf(marathon))
        given(courseRepository.findAllById(setOf(200L)))
            .willReturn(listOf(course))

        val result = registrationService.getMyRegistrations(
            userId,
            MyRegistrationStatusFilter.CANCELED,
            pageable
        )

        val pageableCaptor = ArgumentCaptor.forClass(Pageable::class.java)

        verify(registrationCancelHistoryRepository).findByUserId(
            eq(userId),
            capturePageable(pageableCaptor)
        )
        verify(registrationRepository, never()).findByUser_Id(
            anyLong(),
            anyPageable()
        )

        val captured = pageableCaptor.value
        assertEquals(1, captured.pageNumber)
        assertEquals(10, captured.pageSize)
        assertEquals(Sort.Direction.DESC, captured.sort.getOrderFor("canceledAt")?.direction)
        assertEquals(Sort.Direction.DESC, captured.sort.getOrderFor("id")?.direction)

        assertNotNull(result)
        assertEquals(1, result.content.size)
        assertEquals(1, result.pageRes.page)
        assertEquals(10, result.pageRes.size)
        assertEquals(11L, result.pageRes.totalElements)
        assertEquals(2, result.pageRes.totalPages)

        val item = result.content[0]
        assertEquals(400L, item.registrationId)
        assertEquals(300L, item.historyId)
        assertEquals(100L, item.marathonId)
        assertEquals("서울 마라톤", item.marathonTitle)
        assertEquals(200L, item.courseId)
        assertEquals("Half", item.courseType)
        assertEquals("CANCELED", item.status)
        assertEquals(BigDecimal("70000"), item.price)
        assertEquals(LocalDate.of(2026, 10, 25), item.eventDate)
        assertEquals("홍길동", item.snapName)
        assertEquals("010-1111-2222", item.snapPhoneNumber)
        assertEquals("54321", item.snapZipCode)
        assertEquals("서울시 송파구", item.snapAddress)
        assertEquals("202동", item.snapDetail)
        assertEquals("M", item.tSize)
        assertEquals(true, item.agreedTerms)
        assertEquals(LocalDateTime.of(2026, 4, 1, 8, 0), item.appliedAt)
        assertEquals(LocalDateTime.of(2026, 4, 5, 18, 30), item.canceledAt)
    }

    @Test
    @DisplayName("내 접수 조회 성공 - ACTIVE 조회 결과가 없으면 빈 목록과 빈 페이지 정보를 반환한다")
    fun getMyRegistrations_active_empty() {
        val userId = 1L
        val pageable = PageRequest.of(0, 20)

        val emptyPage = PageImpl<Registration>(emptyList(), PageRequest.of(0, 20), 0)

        given(registrationRepository.findByUser_Id(eq(userId), anyPageable()))
            .willReturn(emptyPage)

        val result = registrationService.getMyRegistrations(
            userId,
            MyRegistrationStatusFilter.ACTIVE,
            pageable
        )

        assertNotNull(result)
        assertEquals(0, result.content.size)
        assertEquals(0, result.pageRes.page)
        assertEquals(20, result.pageRes.size)
        assertEquals(0L, result.pageRes.totalElements)
        assertEquals(0, result.pageRes.totalPages)
    }

    private fun createUser(id: Long, name: String, phoneNumber: String): Users {
        val user = Users.create(
            email = "test@test.com",
            name = name,
            phoneNumber = phoneNumber,
            gender = Gender.MALE,
            birth = LocalDate.of(1999, 1, 1)
        )

        ReflectionTestUtils.setField(user, "id", id)
        return user
    }

    private fun createMarathon(): Marathon =
        Marathon.create(
            createUser(99L, "주최자", "010-9999-9999"),
            "서울 마라톤",
            "서울",
            "잠실종합운동장",
            LocalDate.of(2026, 10, 25),
            "poster.png",
            LocalDateTime.of(2026, 4, 1, 0, 0),
            LocalDateTime.of(2026, 9, 30, 23, 59)
        )

    private fun createCourse(
        marathon: Marathon,
        courseType: String,
        price: Int,
        capacity: Int,
        currentCount: Int
    ): Course {
        val course = Course.create(
            courseType,
            BigDecimal.valueOf(price.toLong()),
            capacity,
            currentCount
        )
        marathon.addCourse(course)
        return course
    }

    private fun anyPageable(): Pageable {
        any(Pageable::class.java)
        return PageRequest.of(0, 20)
    }

    private fun capturePageable(captor: ArgumentCaptor<Pageable>): Pageable {
        captor.capture()
        return PageRequest.of(0, 20)
    }
}