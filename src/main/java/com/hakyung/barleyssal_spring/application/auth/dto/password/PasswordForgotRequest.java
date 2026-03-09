package com.hakyung.barleyssal_spring.application.auth.dto.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordForgotRequest(
        @NotBlank @Email String email
) {
}
