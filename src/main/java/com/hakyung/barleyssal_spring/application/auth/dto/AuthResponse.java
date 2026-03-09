package com.hakyung.barleyssal_spring.application.auth.dto;

import com.hakyung.barleyssal_spring.domain.user.Role;
import com.hakyung.barleyssal_spring.domain.user.User;
import com.hakyung.barleyssal_spring.global.security.CustomUserDetails;

public record AuthResponse(
    Long id,
    String email,
    Role role,
    String userName,
    boolean active
) {
    public static AuthResponse from(User user) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getUserName(),
                user.isActive()
        );
    }
    public static AuthResponse from(CustomUserDetails userDetails) {
        return new AuthResponse(
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getRole(),
                userDetails.getUserName(),
                userDetails.isActive()
        );
    }
}
