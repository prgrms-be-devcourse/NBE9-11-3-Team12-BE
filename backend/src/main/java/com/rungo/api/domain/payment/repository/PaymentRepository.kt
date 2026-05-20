package com.rungo.api.domain.payment.repository

import com.rungo.api.domain.payment.entity.Payment
import com.rungo.api.domain.payment.enumtype.PaymentStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PaymentRepository : JpaRepository<Payment, Long> {

    fun findByOrderId(orderId: String): Payment?

    fun findByOriginalRegistrationId(originalRegistrationId: Long): Payment?

    fun findByOriginalRegistrationIdIn(originalRegistrationIds: Collection<Long>): List<Payment>

    fun findByOriginalRegistrationIdInAndStatus(
        originalRegistrationIds: Collection<Long>,
        status: PaymentStatus,
    ): List<Payment>

    // confirm 요청 중복 방지를 위한 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId = :orderId")
    fun findByOrderIdForUpdate(@Param("orderId") orderId: String): Payment?

    // 결제 상태 변경 시 동시성 제어를 위한 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Payment?

    // 접수 단위 결제 취소/환불 처리를 위한 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.originalRegistrationId = :originalRegistrationId")
    fun findByOriginalRegistrationIdForUpdate(
        @Param("originalRegistrationId") originalRegistrationId: Long,
    ): Payment?

    // 여러 접수의 결제를 일괄 취소/환불하기 위한 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.originalRegistrationId in :originalRegistrationIds")
    fun findByOriginalRegistrationIdInForUpdate(
        @Param("originalRegistrationIds") originalRegistrationIds: Collection<Long>,
    ): List<Payment>

    // 만료 시간이 지난 결제 ID 조회
    @Query(
        """
        select p.id
        from Payment p
        where p.status = :status
          and p.expiresAt <= :now
        """
    )
    fun findIdsByStatusAndExpiresAtLessThanEqual(
        @Param("status") status: PaymentStatus,
        @Param("now") now: LocalDateTime,
    ): List<Long>

    // 만료 시간이 지난 결제 ID를 오래된 순서로 제한 조회
    @Query(
        """
    select p.id
    from Payment p
    where p.status = :status
      and p.expiresAt <= :now
    order by p.expiresAt asc, p.id asc
    """
    )
    fun findIdsByStatusAndExpiresAtLessThanEqual(
        @Param("status") status: PaymentStatus,
        @Param("now") now: LocalDateTime,
        pageable: Pageable,
    ): List<Long>

    // 특정 상태의 결제 ID를 오래된 수정 순서로 조회
    @Query(
        """
    select p.id
    from Payment p
    where p.status in :statuses
    order by p.updatedAt asc, p.id asc
    """
    )
    fun findIdsByStatusIn(
        @Param("statuses") statuses: Collection<PaymentStatus>,
        pageable: Pageable,
    ): List<Long>

    // 일정 시간 이상 처리 중인 결제 ID 조회
    @Query(
        """
    select p.id
    from Payment p
    where p.status = :status
      and p.updatedAt <= :cutoff
    order by p.updatedAt asc, p.id asc
    """
    )
    fun findIdsByStatusAndUpdatedAtLessThanEqual(
        @Param("status") status: PaymentStatus,
        @Param("cutoff") cutoff: LocalDateTime,
        pageable: Pageable,
    ): List<Long>
}

