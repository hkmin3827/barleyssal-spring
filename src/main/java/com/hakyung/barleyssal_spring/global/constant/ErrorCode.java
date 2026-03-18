package com.hakyung.barleyssal_spring.global.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 이메일입니다."),
    PHONE_NUMBER_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 번호입니다."),
    AUTH_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호를 확인해주세요."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."),
    HOLDING_NOT_FOUND(HttpStatus.NOT_FOUND,"주식 종목을 찾을 수 없습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "거래 주문을 찾을 수 없습니다."),
    DELETED_ACCOUNT(HttpStatus.CONFLICT, "회원 탈퇴한 계정입니다."),
    INACTIVE_USER(HttpStatus.CONFLICT, "비활성화된 계정입니다. 관리자에게 문의해주세요."),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    INVALID_RESET_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 비밀번호 변경 토큰입니다. 다시 시도해주세요."),
    INACTIVE_USER_ALREADY(HttpStatus.CONFLICT, "이미 비활성화된 사용자입니다."),
    ACTIVE_USER_ALREADY(HttpStatus.CONFLICT, "이미 활성화된 사용자입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INSUFFICIENT_DEPOSIT(HttpStatus.CONFLICT, "잔액이 부족합니다."),
    NOT_ENOUGH_STOCK_QUANTITY_TO_SELL(HttpStatus.BAD_REQUEST, "매도 주식 수량이 현재 보유한 수량을 초과했습니다."),
    UNSUITABLE_ORDER_STATUS(HttpStatus.CONFLICT, "기존 주문 상태가 요청에 적합하지 않습니다."),
    ORDER_FILLED_ALREADY(HttpStatus.CONFLICT, "이미 체결된 주문건입니다."),
    ORDER_CANCELLED_ALREADY(HttpStatus.CONFLICT, "이미 취소된 주문건입니다."),
    LIMIT_PRICE_REQUIRED(HttpStatus.BAD_REQUEST, "지정가 주문은 지정가 입력이 필요합니다."),
    INVALID_EXECUTED_QUANTITY(HttpStatus.CONFLICT, "주문 매도 수량보다 매도 수량이 큽니다. 관리자 확인이 필요합니다."),
    INVALID_PRINCIPAL_UNIT(HttpStatus.BAD_REQUEST, "원금은 10원 단위로 입력해야합니다."),
    INVALID_REQUEST_QUANTITY(HttpStatus.CONFLICT, "주문했던 주식 수가 취소 요청한 수보다 작습니다. 관리자 확인이 필요합니다."),
    ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리되었거나 변경된 주문입니다."),
    MARKET_CLOSED(HttpStatus.BAD_REQUEST, "장 운영 시간이 아닙니다."),
    WATCHLIST_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "관심종목은 최대 40개까지 등록할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
