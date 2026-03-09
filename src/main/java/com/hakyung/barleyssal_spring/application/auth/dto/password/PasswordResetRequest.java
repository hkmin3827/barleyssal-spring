package com.hakyung.barleyssal_spring.application.auth.dto.password;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        String resetToken,
        @NotBlank(message = "새 비밀번호를 반드시 입력해주세요.") String newPassword
) {
}
