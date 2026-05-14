package com.rungo.api.global.exception;

import com.rungo.api.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REGISTRATION_DUPLICATE_CONSTRAINT = "uk_registration_user_marathon";
    private static final String REGISTRATION_CANCEL_HISTORY_DUPLICATE_CONSTRAINT =
            "uk_registration_cancel_history_original_registration_id";
    private static final String MARATHON_DUPLICATE_CONSTRAINT = "uk_marathon_organizerId_title_eventDate";

    // 1. 비즈니스 예외 처리 (CustomException)
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("Business Exception: {}", e.getErrorCode().getMessage());
        ErrorCode ec = e.getErrorCode();

        // 토큰 재발급 중복 요청 시 클라이언트 재시도 안내
        if (ec == ErrorCode.TOKEN_REISSUE_IN_PROGRESS) {
            return ResponseEntity.status(ec.getStatus())
                    .header("Retry-After", "1") // 1초 후 재시도
                    .body(ApiResponse.error(ec.getStatus(), ec.name(), ec.getMessage()));
        }

        return ResponseEntity.status(ec.getStatus())
                             .body(ApiResponse.error(ec.getStatus(), ec.name(), ec.getMessage()));
    }

    // 2. 입력값 검증 실패 처리 (@Valid RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Validation Exception 발생");
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            // 중복 필드 에러 덮어쓰기 방지
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        // ErrorCode 재사용
        ErrorCode ec = ErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(ec.getStatus())
                             .body(ApiResponse.error(ec.getStatus(), ec.name(), ec.getMessage(), errors));
    }

    // 3. 파라미터 검증 실패 처리 (@Validated RequestParam, PathVariable)
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("ConstraintViolationException 발생");
        Map<String, String> errors = new HashMap<>();

        e.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
            // 중복 방지
            errors.putIfAbsent(fieldName, violation.getMessage());
        });

        // ErrorCode 재사용
        ErrorCode ec = ErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity.status(ec.getStatus())
                             .body(ApiResponse.error(ec.getStatus(), ec.name(), ec.getMessage(), errors));
    }

    // 4. DB 유니크 제약 예외 처리
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        // registration 중복 접수 제약조건 위반은 비즈니스 예외로 변환한다.
        if (isConstraintViolation(e, REGISTRATION_DUPLICATE_CONSTRAINT)) {
            ErrorCode ec = ErrorCode.REGISTRATION_ALREADY_EXISTS;

            log.warn("Duplicate registration detected. constraintName={}",
                    REGISTRATION_DUPLICATE_CONSTRAINT);

            return ResponseEntity.status(ec.getStatus())
                    .body(ApiResponse.error(ec.getStatus(), ec.name(), ec.getMessage()));
        }

        if (isConstraintViolation(e, REGISTRATION_CANCEL_HISTORY_DUPLICATE_CONSTRAINT)) {
            ErrorCode ec = ErrorCode.REGISTRATION_ALREADY_CANCELED;

            log.warn("Duplicate registration cancel history detected. constraintName={}",
                    REGISTRATION_CANCEL_HISTORY_DUPLICATE_CONSTRAINT);

            return ResponseEntity.status(ec.getStatus())
                    .body(ApiResponse.error(ec.getStatus(), ec.name(), ec.getMessage()));
        }

        if (isConstraintViolation(e, MARATHON_DUPLICATE_CONSTRAINT)) {
            ErrorCode ec = ErrorCode.MARATHON_ALREADY_EXISTS;

            log.warn("Duplicate marathon detected. constraintName={}",
                    MARATHON_DUPLICATE_CONSTRAINT);

            return ResponseEntity.status(ec.getStatus())
                    .body(ApiResponse.error(ec.getStatus(), ec.name(), ec.getMessage()));
        }

        // 처리 대상이 아닌 다른 DB 무결성 예외는 시스템 예외로 응답한다.
        log.error("Unhandled DataIntegrityViolationException", e);
        ErrorCode ec = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.error(ec.getStatus(), ec.name(), ec.getMessage()));
    }

    // 5. 시스템 예외 처리
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Internal Server Error: ", e);

        ErrorCode ec = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(ec.getStatus())
                             .body(ApiResponse.error(ec.getStatus(), ec.name(), ec.getMessage()));
    }

    // 예외 원인 체인을 따라가며 기대한 DB 제약조건 위반인지 확인한다.
    private boolean isConstraintViolation(Throwable throwable, String expectedConstraintName) {
        Throwable current = throwable;

        while (current != null) {
            // Hibernate 예외까지 내려가 실제 제약조건명을 확인한다.
            if (current instanceof org.hibernate.exception.ConstraintViolationException ex) {
                String actualConstraintName = ex.getConstraintName();
                return actualConstraintName != null && actualConstraintName.endsWith(expectedConstraintName);
            }
            current = current.getCause();
        }

        return false;
    }


}
