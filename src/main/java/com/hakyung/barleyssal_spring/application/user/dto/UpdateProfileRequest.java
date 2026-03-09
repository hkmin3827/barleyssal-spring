package com.hakyung.barleyssal_spring.application.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileRequest(
        @NotBlank(message = "사용자 이름은 필수값입니다.")
        String userName,

        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 번호 형식이 아닙니다.")
        String phoneNumber
) {}
