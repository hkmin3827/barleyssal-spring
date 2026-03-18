package com.hakyung.barleyssal_spring.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // 영문 + 숫자 + 특수문자(~ ! @ # $ % ^ & *) 3조합, 8자 이상
    private static final java.util.regex.Pattern PASSWORD_PATTERN =
            java.util.regex.Pattern.compile(
                    "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*]).{8,}$"
            );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return PASSWORD_PATTERN.matcher(value).matches();
    }
}
