package com.hakyung.barleyssal_spring.global.exception;

import com.hakyung.barleyssal_spring.global.constant.ErrorCode;

public class AccountNotFoundException extends CustomException {
    public AccountNotFoundException() { super(ErrorCode.ACCOUNT_NOT_FOUND); }
}
