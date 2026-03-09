package com.hakyung.barleyssal_spring.domain.user;

import com.hakyung.barleyssal_spring.application.auth.dto.SignupRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String encodedPassword;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.ROLE_USER;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true, unique = true)
    private String phoneNumber;

    @Column(nullable = true)
    private LocalDateTime deletedAt;

    private User(String email, String password, String userName) {
        this.email = email;
        this.encodedPassword = password;
        this.userName = userName;
        this.role = Role.ROLE_USER;
        this.createdAt = LocalDateTime.now();
    }

    public static User of(SignupRequest req, String encodedPassword) {
        User user = new User();
        user.email = req.email();
        user.encodedPassword = encodedPassword;
        user.userName = req.userName();
        user.role = Role.ROLE_USER;
        user.phoneNumber = req.phoneNumber();
        user.createdAt = LocalDateTime.now();
        user.active = true;
        return user;
    }


    public void updateProfile(String userName, String phoneNumber) {
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        if (encodedPassword != null && !encodedPassword.isBlank()) {
            this.encodedPassword = encodedPassword;
        }
    }

    public void changePassword(String newEncodedPassword) {
        this.encodedPassword = newEncodedPassword;
    }

    public void activate() { this.active = true; }
    public void deactivate() { this.active = false; }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
