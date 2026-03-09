package com.hakyung.barleyssal_spring.application.auth.dto;


import com.hakyung.barleyssal_spring.domain.user.Role;
import com.hakyung.barleyssal_spring.domain.user.User;

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
}
