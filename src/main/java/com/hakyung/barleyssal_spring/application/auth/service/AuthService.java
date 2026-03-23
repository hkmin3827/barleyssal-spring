package com.hakyung.barleyssal_spring.application.auth.service;

import com.hakyung.barleyssal_spring.application.auth.dto.SignupRequest;
import com.hakyung.barleyssal_spring.application.auth.dto.WithdrawRequest;
import com.hakyung.barleyssal_spring.domain.user.User;
import com.hakyung.barleyssal_spring.domain.user.UserRepository;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import com.hakyung.barleyssal_spring.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(SignupRequest req) {
        if(userRepository.existsByEmail(req.email())){
            throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
        }
        if(userRepository.existsByPhoneNumberAndDeletedAtIsNull(req.phoneNumber())){
            throw new CustomException(ErrorCode.PHONE_NUMBER_DUPLICATED);
        }

        String encodedPassword = passwordEncoder.encode(req.password());

        User user = User.of(req, encodedPassword);
        return userRepository.save(user);
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest req) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.getDeletedAt() != null) {throw new CustomException(ErrorCode.DELETED_ACCOUNT);}
        if (!"탈퇴합니다.".equals(req.confirmText())) {
            throw new IllegalArgumentException("입력하신 탈퇴 확인 문구가 틀립니다.");
        }
        if(!passwordEncoder.matches(req.password(), user.getEncodedPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        user.softDelete();
    }
}
