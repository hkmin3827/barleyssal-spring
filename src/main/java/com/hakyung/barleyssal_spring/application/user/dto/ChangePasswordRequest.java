package com.hakyung.barleyssal_spring.application.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "새 비밀번호가 입력되지 않았습니다.") String newPassword,
        @NotBlank(message = "비밀번호 확인 값을 입력해주세요.") String confirmNewPassword
) {}
