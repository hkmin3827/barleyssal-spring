package com.hakyung.barleyssal_spring.global.security;

import com.hakyung.barleyssal_spring.domain.user.Role;
import com.hakyung.barleyssal_spring.domain.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    private final Long userId;
    private final String email;
    private final String userName;
    private final String password; // DB - encodedPassword
    private final Role role;
    private final boolean active;
    private final boolean deleted;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.userName = user.getUserName();
        this.password = user.getEncodedPassword();
        this.role = user.getRole();
        this.active = user.isActive();
        this.deleted = user.getDeletedAt() != null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) return List.of();
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }
    @Override public String getPassword() { return password; }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !deleted;
    }

    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    public Role getRole() { return role; }
    public String getEmail() { return email; }
    public String getUserName() { return userName; }
    public Long getId() { return userId; }
    public boolean isActive() { return active; }

}
