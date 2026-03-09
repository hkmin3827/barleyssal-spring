package com.hakyung.barleyssal_spring.global.exception;


import com.hakyung.barleyssal_spring.global.constant.ErrorCode;

public class UserNotFoundException extends CustomException{
    public UserNotFoundException(){
        super(ErrorCode.USER_NOT_FOUND);
    }
}
