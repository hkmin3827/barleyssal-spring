package com.hakyung.barleyssal_spring.application.account.dto;

import com.hakyung.barleyssal_spring.domain.account.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
    Long id,
    Long userId,
    BigDecimal principal,
    BigDecimal deposit,
    Instant createdAt,
    Instant updatedAt
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(
            a.getId(), a.getUserId(),
            a.getPrincipal(), a.getDeposit(),
            a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
