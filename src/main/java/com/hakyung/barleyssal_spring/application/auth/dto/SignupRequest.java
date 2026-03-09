package com.hakyung.barleyssal_spring.application.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupRequest (
        @NotBlank(message = "이메일은 필수 값입니다.") @Email
        String email,

        @NotBlank
        String password,

        @NotBlank(message = "사용자 이름은 필수 값입니다.")
        String userName,

        @NotBlank(message = "전화번호는 필수 값입니다.")
        @Pattern(
                regexp = "^010[0-9]{8}$",
                message = "전화번호는 010으로 시작하는 11자리 숫자여야 합니다."
        )
        String phoneNumber
) {}