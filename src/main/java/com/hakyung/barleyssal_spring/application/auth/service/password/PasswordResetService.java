package com.hakyung.barleyssal_spring.application.auth.service.password;

import com.hakyung.barleyssal_spring.domain.user.User;
import com.hakyung.barleyssal_spring.domain.user.UserRepository;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import com.hakyung.barleyssal_spring.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final String RESET_PREFIX = "RESET:";
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final MailService mailService;
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.base-url}")
    private String baseUrl;

    public String createResetToken(Long userId) {
        String resetToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                RESET_PREFIX + resetToken,
                userId.toString(),
                10,
                TimeUnit.MINUTES
        );
        return resetToken;
    }

    public Long getUserIdByResetToken(String resetToken) {
        Object userId = redisTemplate.opsForValue().get(RESET_PREFIX + resetToken);
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    public void sendResetLink(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
        if(user.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.DELETED_ACCOUNT);
        }

        String resetToken = createResetToken(user.getId());

        String resetUrl =
                baseUrl + "/reset-password?token=" + resetToken;

        mailService.sendPasswordResetMail(email, resetUrl);
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        Long userId = getUserIdByResetToken(resetToken);

        if(userId == null) {
            throw new CustomException(ErrorCode.INVALID_RESET_TOKEN);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encodedNewPassword);

        redisTemplate.delete(RESET_PREFIX + resetToken);
    }
}
