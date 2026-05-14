package com.rungo.api.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 에러가 발생했습니다."),

    // 유저, 인증
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ALREADY_ORGANIZER(HttpStatus.BAD_REQUEST, "이미 주최자 권한을 가진 회원입니다."),
    PROFILE_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "필수 프로필 정보가 누락되었습니다."),

    // 토큰
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "리프레시 토큰이 존재하지 않습니다."),
    TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 액세스 토큰입니다."),
    TOKEN_REISSUE_IN_PROGRESS(HttpStatus.CONFLICT, "이미 토큰 재발급이 진행 중입니다."),
    TOKEN_REISSUE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 재발급에 실패했습니다."),

    // 마라톤 대회 등록
    MARATHON_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 동일한 대회가 존재합니다."),
    MARATHON_CANCELED(HttpStatus.BAD_REQUEST, "취소된 대회입니다."),
    MARATHON_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "이미 취소된 대회입니다."),

    // 마라톤, 접수
    MARATHON_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 마라톤 대회를 찾을 수 없습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 코스를 찾을 수 없습니다."),
    REGISTRATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 접수 내역을 찾을 수 없습니다."),
    REGISTRATION_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "접수 가능 기간이 아닙니다."),
    REGISTRATION_CANCEL_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "접수 취소 가능 기간이 아닙니다."),
    MARATHON_NOT_OPEN(HttpStatus.BAD_REQUEST, "현재 접수 가능한 대회 상태가 아닙니다."),
    REGISTRATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 접수한 대회입니다."),
    REGISTRATION_TERMS_REQUIRED(HttpStatus.BAD_REQUEST, "필수 약관 동의가 필요합니다."),
    CAPACITY_FULL(HttpStatus.BAD_REQUEST, "마라톤 참가 정원이 마감되었습니다."),
    REGISTRATION_ALREADY_CANCELED(HttpStatus.BAD_REQUEST,"이미 접수 취소한 대회입니다."),
    CURRENT_COUNT_UNDERFLOW(HttpStatus.BAD_REQUEST,"코스 정원이 이미 0명입니다.");
    private final HttpStatus status;
    private final String message;
}
