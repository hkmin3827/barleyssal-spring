package com.hakyung.barleyssal_spring.application.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SetPrincipalRequest(
    @NotNull @DecimalMin("1000") BigDecimal principal
) {}
