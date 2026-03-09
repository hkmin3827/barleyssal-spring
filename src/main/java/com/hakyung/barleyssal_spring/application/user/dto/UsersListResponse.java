package com.hakyung.barleyssal_spring.application.user.dto;

public interface UsersListResponse{
    Long getId();
    String getEmail();
    String getUserName();
    boolean isActive();
}
