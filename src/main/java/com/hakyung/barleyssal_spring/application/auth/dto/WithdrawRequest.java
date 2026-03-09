package com.hakyung.barleyssal_spring.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
        @NotBlank(message = "탈퇴 확인을 위한 문구를 입력해주세요.") String confirmText,
        @NotBlank(message = "회원 비밀번호가 입력되지 않았습니다.") String password
) {}
