package com.hakyung.barleyssal_spring.application.user.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordVerifyRequest(
        @NotBlank(message = "비밀번호를 입력해주세요.") String password
) {}
