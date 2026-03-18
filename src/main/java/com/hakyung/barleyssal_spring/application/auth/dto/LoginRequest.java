package com.hakyung.barleyssal_spring.application.auth.dto;

import com.hakyung.barleyssal_spring.global.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email  @NotBlank(message = "이메일을 입력해주세요.") String email,
        @NotBlank(message = "비밀번호를 입력해주세요.") @ValidPassword String password
) {}
