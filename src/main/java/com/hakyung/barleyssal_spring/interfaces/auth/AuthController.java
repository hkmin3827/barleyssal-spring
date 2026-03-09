package com.hakyung.barleyssal_spring.interfaces.auth;

import com.hakyung.barleyssal_spring.application.auth.dto.AuthResponse;
import com.hakyung.barleyssal_spring.application.auth.dto.LoginRequest;
import com.hakyung.barleyssal_spring.application.auth.dto.SignupRequest;
import com.hakyung.barleyssal_spring.application.auth.dto.WithdrawRequest;
import com.hakyung.barleyssal_spring.application.auth.dto.password.PasswordForgotRequest;
import com.hakyung.barleyssal_spring.application.auth.dto.password.PasswordResetRequest;
import com.hakyung.barleyssal_spring.application.auth.service.AuthService;
import com.hakyung.barleyssal_spring.application.auth.service.password.PasswordResetService;
import com.hakyung.barleyssal_spring.domain.user.User;
import com.hakyung.barleyssal_spring.global.jwt.JwtProvider;
import com.hakyung.barleyssal_spring.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtProvider jwtProvider;
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginReq, HttpServletRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginReq.email(), loginReq.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtProvider.createAccessToken(userDetails.getId(), userDetails.getRole());

        HttpSession session = req.getSession(true);
        session.setAttribute("ACCESS_TOKEN", accessToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        AuthResponse response = AuthResponse.from(userDetails);

        log.info("JWT Issued & Session Stored: userId={}, token={}", userDetails.getId(), accessToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest signupReq, HttpServletRequest req) {
        User user = authService.signup(signupReq);

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        HttpSession session = req.getSession(true);
        session.setAttribute("ACCESS_TOKEN", accessToken);
        return ResponseEntity.ok(AuthResponse.from(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate(); // Redis에서 세션 삭제
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @Valid @RequestBody WithdrawRequest withdrawReq, HttpServletRequest req,
            @AuthenticationPrincipal CustomUserDetails user) {

        authService.withdraw(user.getId(), withdrawReq);

        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate(); // Redis에서 세션 삭제
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(
            @RequestBody PasswordForgotRequest req
    ) {
        passwordResetService.sendResetLink(req.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(
            @RequestBody PasswordResetRequest req
    ) {
        passwordResetService.resetPassword(
                req.resetToken(),
                req.newPassword()
        );
        return ResponseEntity.ok().build();
    }
}
