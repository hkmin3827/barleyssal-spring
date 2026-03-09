package com.hakyung.barleyssal_spring.interfaces.admin;

import com.hakyung.barleyssal_spring.application.user.UserService;
import com.hakyung.barleyssal_spring.application.user.dto.UserDetailResponse;
import com.hakyung.barleyssal_spring.application.user.dto.UsersListResponse;
import com.hakyung.barleyssal_spring.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @GetMapping
    public PageResponse<UsersListResponse> getUsersByStatus(
            @RequestParam(required = true) boolean active,
            @PageableDefault(size = 20)Pageable pageable
            ) {
        return PageResponse.from(userService.getUsersByActive(active, pageable));
    }

    @GetMapping("/{userId}")
    public UserDetailResponse getUserDetail(@PathVariable Long userId) {
        return userService.getUserDetail(userId);
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long userId) {
        userService.activate(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {
        userService.deactivate(userId);
        return ResponseEntity.noContent().build();
    }
}
