package com.rungo.api.global.exception

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.KotlinInvalidNullException
import com.rungo.api.global.response.ApiResponse
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    // 1. 비즈니스 예외 처리 (CustomException)
    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ApiResponse<Void?>> {
        val ec = e.errorCode
        log.warn("Business Exception: {}", ec.message)

        val response = ApiResponse.error<Void?>(ec.status, ec.name, ec.message)

        // 토큰 재발급 중복 요청 시 클라이언트 재시도 안내
        return if (ec == ErrorCode.TOKEN_REISSUE_IN_PROGRESS) {
            ResponseEntity.status(ec.status)
                .header("Retry-After", "1") // 1초 후 재시도
                .body(response)
        } else {
            ResponseEntity.status(ec.status)
                .body(response)
        }
    }

    // 2. 입력값 검증 실패 처리 (@Valid RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        e: MethodArgumentNotValidException
    ): ResponseEntity<ApiResponse<Map<String, String>>> {
        log.warn("Validation Exception 발생")

        // 중복 필드 에러 덮어쓰기 방지 (가장 첫 에러만 유지)
        val errors = e.bindingResult.fieldErrors
            .distinctBy { it.field }
            .associate { it.field to (it.defaultMessage ?: "잘못된 입력입니다.") }

        // ErrorCode 재사용
        val ec = ErrorCode.INVALID_INPUT_VALUE

        return ResponseEntity.status(ec.status)
            .body(ApiResponse.error(ec.status, ec.name, ec.message, errors))
    }

    // 3. 파라미터 검증 실패 처리 (@Validated RequestParam, PathVariable)
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        e: ConstraintViolationException
    ): ResponseEntity<ApiResponse<Map<String, String>>> {
        log.warn("ConstraintViolationException 발생")

        // 중복 방지 및 마지막 변수명 추출
        val errors = e.constraintViolations
            .distinctBy { it.propertyPath.toString().substringAfterLast('.') }
            .associate {
                val fieldName = it.propertyPath.toString().substringAfterLast('.')
                fieldName to it.message
            }

        // ErrorCode 재사용
        val ec = ErrorCode.INVALID_INPUT_VALUE

        return ResponseEntity.status(ec.status)
            .body(ApiResponse.error(ec.status, ec.name, ec.message, errors))
    }

    // 4. 요청 바디를 DTO로 바꾸는 단계에서 실패한 경우 처리
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        e: HttpMessageNotReadableException
    ): ResponseEntity<ApiResponse<Void?>> {
        log.warn("HttpMessageNotReadableException 발생: {}", e.message)

        val ec = ErrorCode.INVALID_INPUT_VALUE

        val message = when (e.cause) {
            is KotlinInvalidNullException -> "필수 입력값이 누락되었습니다."
            is InvalidFormatException -> "입력값 형식이 올바르지 않습니다."
            else -> "요청 형식이 올바르지 않습니다."
        }

        return ResponseEntity.status(ec.status)
            .body(ApiResponse.error(ec.status, ec.name, message))
    }

    // 5. DB 유니크 제약조건 위반 처리
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        e: DataIntegrityViolationException
    ): ResponseEntity<ApiResponse<Void?>> {
        val ec = DataIntegrityViolationErrorCodeResolver.resolve(e) ?: run {
            log.error("Unhandled DataIntegrityViolationException", e)
            ErrorCode.INTERNAL_SERVER_ERROR
        }

        return ResponseEntity.status(ec.status)
            .body(ApiResponse.error(ec.status, ec.name, ec.message))
    }

    // 6. 시스템 예외 처리
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Void?>> {
        log.error("Internal Server Error: ", e)

        val ec = ErrorCode.INTERNAL_SERVER_ERROR

        return ResponseEntity.status(ec.status)
            .body(ApiResponse.error(ec.status, ec.name, ec.message))
    }
}
