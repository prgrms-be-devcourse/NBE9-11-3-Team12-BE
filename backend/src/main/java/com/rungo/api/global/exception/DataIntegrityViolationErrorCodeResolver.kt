package com.rungo.api.global.exception

import org.springframework.dao.DataIntegrityViolationException

object DataIntegrityViolationErrorCodeResolver {

    private const val REGISTRATION_DUPLICATE_CONSTRAINT = "uk_registration_user_marathon"
    private const val REGISTRATION_CANCEL_HISTORY_DUPLICATE_CONSTRAINT =
        "uk_registration_cancel_history_original_registration_id"
    private const val MARATHON_DUPLICATE_CONSTRAINT = "uk_marathon_organizerId_title_eventDate"

    fun resolve(exception: DataIntegrityViolationException): ErrorCode? = when {
        isConstraintViolation(exception, REGISTRATION_DUPLICATE_CONSTRAINT) -> ErrorCode.REGISTRATION_ALREADY_EXISTS
        isConstraintViolation(
            exception,
            REGISTRATION_CANCEL_HISTORY_DUPLICATE_CONSTRAINT
        ) -> ErrorCode.REGISTRATION_ALREADY_CANCELED

        isConstraintViolation(exception, MARATHON_DUPLICATE_CONSTRAINT) -> ErrorCode.MARATHON_ALREADY_EXISTS
        else -> null
    }

    // 예외 원인 체인을 따라가며 기대한 DB 제약조건 위반인지 확인한다.
    private fun isConstraintViolation(
        throwable: Throwable,
        expectedConstraintName: String
    ): Boolean {
        var current: Throwable? = throwable

        while (current != null) {
            // Hibernate 예외까지 내려가 실제 제약조건명을 확인한다.
            if (current is org.hibernate.exception.ConstraintViolationException) {
                val actualConstraintName = current.constraintName

                if (actualConstraintName != null &&
                    actualConstraintName.endsWith(expectedConstraintName)
                ) {
                    return true
                }
            }

            current = current.cause
        }

        return false
    }
}
