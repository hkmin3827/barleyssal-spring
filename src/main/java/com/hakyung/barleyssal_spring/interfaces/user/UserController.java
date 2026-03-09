package com.hakyung.barleyssal_spring.interfaces.user;

import com.hakyung.barleyssal_spring.application.user.UserService;
import com.hakyung.barleyssal_spring.application.user.dto.ChangePasswordRequest;
import com.hakyung.barleyssal_spring.application.user.dto.PasswordVerifyRequest;
import com.hakyung.barleyssal_spring.application.user.dto.UpdateProfileRequest;
import com.hakyung.barleyssal_spring.application.user.dto.UserDetailResponse;
import com.hakyung.barleyssal_spring.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public UserDetailResponse getMe(@AuthenticationPrincipal CustomUserDetails user) {
        return userService.getMe(user.getId());
    }

    @PostMapping("/me/password-verify")
    public ResponseEntity<Void> passwordVerify(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody PasswordVerifyRequest req
    ) {
        userService.verifyPassword(user.getId(), req);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UpdateProfileRequest req
            ) {
        userService.updateProfile(user.getId(), req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/password-change")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ChangePasswordRequest req
            ) {
        userService.changePassword(user.getId(), req.newPassword(), req.confirmNewPassword());
        return ResponseEntity.noContent().build();
    }
}
