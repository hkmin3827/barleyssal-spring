package com.hakyung.barleyssal_spring.application.user.dto;

import com.hakyung.barleyssal_spring.domain.user.User;

import java.time.LocalDateTime;

public record UserDetailResponse(
        String email,
        String userName,
        String phoneNumber,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
    public static UserDetailResponse from(User user) {
        return new UserDetailResponse(
                user.getEmail(),
                user.getUserName(),
                user.getPhoneNumber(),
                user.isActive(),
                user.getCreatedAt(),
                user.getDeletedAt()
        );
    }
}
