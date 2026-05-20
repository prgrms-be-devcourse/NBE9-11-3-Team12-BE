package com.rungo.api.domain.marathon.marathon.service

import com.rungo.api.domain.marathon.marathon.entity.Marathon
import com.rungo.api.domain.marathon.marathon.repository.MarathonRepository
import com.rungo.api.domain.registration.repository.RegistrationCancelHistoryRepository
import com.rungo.api.domain.registration.repository.RegistrationRepository
import com.rungo.api.domain.users.entity.Users
import com.rungo.api.domain.users.enumtype.Gender
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class MarathonCleanupServiceTest {

    @InjectMocks
    private lateinit var marathonCleanupService: MarathonCleanupService

    @Mock
    private lateinit var marathonRepository: MarathonRepository

    @Mock
    private lateinit var registrationRepository: RegistrationRepository

    @Mock
    private lateinit var registrationCancelHistoryRepository: RegistrationCancelHistoryRepository

    @Test
    @DisplayName("5년이 지난 대회가 없으면 아무것도 삭제하지 않는다")
    fun cleanup_does_nothing_when_no_old_marathons() {
        given(marathonRepository.findIdsByEventDateBefore(LocalDate.now().minusYears(5)))
            .willReturn(emptyList())

        marathonCleanupService.cleanup()

        verifyNoInteractions(registrationCancelHistoryRepository, registrationRepository)
        verify(marathonRepository, never()).findAllById(Mockito.anyIterable())
        verify(marathonRepository, never()).deleteAll(Mockito.anyIterable())
    }

    @Test
    @DisplayName("5년이 지난 대회가 있으면 취소 이력 → 참가 신청 → 대회 순으로 삭제한다")
    fun cleanup_deletes_in_correct_order_when_old_marathons_exist() {
        val fiveYearsAgo = LocalDate.now().minusYears(5)
        val oldIds = listOf(1L, 2L)
        val oldMarathons = listOf(
            createMarathon(eventDate = LocalDate.now().minusYears(6)).also { setField(it, "id", 1L) },
            createMarathon(eventDate = LocalDate.now().minusYears(6)).also { setField(it, "id", 2L) },
        )

        given(marathonRepository.findIdsByEventDateBefore(fiveYearsAgo)).willReturn(oldIds)
        given(marathonRepository.findAllById(oldIds)).willReturn(oldMarathons)

        marathonCleanupService.cleanup()

        val inOrder = Mockito.inOrder(registrationCancelHistoryRepository, registrationRepository, marathonRepository)
        inOrder.verify(registrationCancelHistoryRepository).deleteAllByMarathonIdIn(oldIds)
        inOrder.verify(registrationRepository).deleteAllByMarathonIdIn(oldIds)
        inOrder.verify(marathonRepository).deleteAll(oldMarathons)
    }

    @Test
    @DisplayName("5년이 지난 대회 ID를 모두 전달하여 삭제한다")
    fun cleanup_passes_correct_ids_to_all_repositories() {
        val fiveYearsAgo = LocalDate.now().minusYears(5)
        val oldIds = listOf(10L, 20L, 30L)
        val oldMarathons = listOf(
            createMarathon(eventDate = LocalDate.now().minusYears(6)).also { setField(it, "id", 10L) },
            createMarathon(eventDate = LocalDate.now().minusYears(6)).also { setField(it, "id", 20L) },
            createMarathon(eventDate = LocalDate.now().minusYears(6)).also { setField(it, "id", 30L) },
        )

        given(marathonRepository.findIdsByEventDateBefore(fiveYearsAgo)).willReturn(oldIds)
        given(marathonRepository.findAllById(oldIds)).willReturn(oldMarathons)

        marathonCleanupService.cleanup()

        verify(registrationCancelHistoryRepository).deleteAllByMarathonIdIn(oldIds)
        verify(registrationRepository).deleteAllByMarathonIdIn(oldIds)
        verify(marathonRepository).deleteAll(oldMarathons)
    }

    private fun createMarathon(eventDate: LocalDate): Marathon =
        Marathon.create(
            organizer = createUser(),
            title = "테스트 마라톤",
            region = "서울",
            detailedAddress = "성동구",
            eventDate = eventDate,
            posterImageUrl = null,
            registrationStartAt = LocalDateTime.now().minusYears(6),
            registrationEndAt = LocalDateTime.now().minusYears(6).plusMonths(1)
        )

    private fun createUser(): Users =
        Users.create(
            email = "test@test.com",
            name = "테스터",
            phoneNumber = "010-1234-5678",
            gender = Gender.MALE,
            birth = LocalDate.of(1990, 1, 1)
        ).also { setField(it, "id", 99L) }

    private fun setField(target: Any, name: String, value: Any?) {
        ReflectionTestUtils.setField(target, name, value)
    }
}