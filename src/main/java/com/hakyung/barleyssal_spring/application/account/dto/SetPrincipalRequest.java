package com.hakyung.barleyssal_spring.application.account.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetPrincipalRequest(
    @NotNull @DecimalMin("1000") BigDecimal principal
) {
    @AssertTrue(message = "원금은 10원 단위로 입력해야 합니다.")
    public boolean isValidPrincipalUnit() {
        if (principal == null) return false;
        return principal.remainder(BigDecimal.TEN).compareTo(BigDecimal.ZERO) == 0;
    }
}
