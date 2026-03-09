package com.hakyung.barleyssal_spring.application.user;

import com.hakyung.barleyssal_spring.application.user.dto.PasswordVerifyRequest;
import com.hakyung.barleyssal_spring.application.user.dto.UpdateProfileRequest;
import com.hakyung.barleyssal_spring.application.user.dto.UserDetailResponse;
import com.hakyung.barleyssal_spring.application.user.dto.UsersListResponse;
import com.hakyung.barleyssal_spring.domain.user.User;
import com.hakyung.barleyssal_spring.domain.user.UserRepository;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import com.hakyung.barleyssal_spring.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void verifyPassword(Long userId, PasswordVerifyRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(req.password(), user.getEncodedPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_NOT_MATCH);
        }
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(UserNotFoundException::new);

        if(!user.getPhoneNumber().equals(req.phoneNumber())) {
            if (userRepository.existsByPhoneNumber(req.phoneNumber())){
                throw new CustomException(ErrorCode.PHONE_NUMBER_DUPLICATED);
            }
        }

        user.updateProfile(req.userName(), req.phoneNumber());
    }

    @Transactional
    public void changePassword(Long userId, String newPassword, String newConfirmPassword) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(UserNotFoundException::new);

        if(!passwordEncoder.matches(newPassword, newConfirmPassword)) {
            throw new CustomException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        String encodedNewPassword =  passwordEncoder.encode(newPassword);
        user.changePassword(encodedNewPassword);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getMe(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(UserNotFoundException::new);

        return UserDetailResponse.from(user);
    }


    // 관리자 호출 메서드
    @Transactional(readOnly = true)
    public Page<UsersListResponse> getUsersByActive(boolean active, Pageable pageable) {
        return userRepository.findUsersByActive(active, pageable);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        return UserDetailResponse.from(user);
    }

    @Transactional
    public void activate(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(UserNotFoundException::new);
        if(user.isActive()) {
            throw new CustomException(ErrorCode.ACTIVE_USER_ALREADY);
        }
        user.activate();
    }

    @Transactional
    public void deactivate(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(UserNotFoundException::new);
        if(!user.isActive()) {
            throw new CustomException(ErrorCode.INACTIVE_USER_ALREADY);
        }
        user.deactivate();
    }
}
