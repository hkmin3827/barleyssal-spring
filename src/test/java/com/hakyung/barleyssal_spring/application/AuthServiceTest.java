package com.hakyung.barleyssal_spring.application;

import com.hakyung.barleyssal_spring.application.auth.dto.SignupRequest;
import com.hakyung.barleyssal_spring.application.auth.dto.WithdrawRequest;
import com.hakyung.barleyssal_spring.application.auth.service.AuthService;
import com.hakyung.barleyssal_spring.domain.user.User;
import com.hakyung.barleyssal_spring.domain.user.UserRepository;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @InjectMocks AuthService authService;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    private SignupRequest signupReq(String email, String phone) {
        return new SignupRequest(email, "Test1234!", "테스터", phone);
    }

    @Nested
    @DisplayName("회원가입 서비스")
    class Signup {
        @Test
        @DisplayName("성공 - 회원가입 성공")
        void success() {
            given(userRepository.existsByEmail(any())).willReturn(false);
            given(userRepository.existsByPhoneNumberAndDeletedAtIsNull(any())).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("encoded");
            given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            User result = authService.signup(signupReq("test@test.com", "01012345678"));

            assertThat(result.getEmail()).isEqualTo("test@test.com");
            assertThat(result.getUserName()).isEqualTo("테스터");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("실패 - 회원가입 시 중복된 이메일을 입력하면 예외를 던진다")
        void duplicate_email_throws() {
            given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

            assertThatThrownBy(() -> authService.signup(signupReq("dup@test.com", "01012345678")))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATED);
        }

        @Test
        @DisplayName("실패 - 회원가입 시 중복된 핸드폰 번호를 입력하면 예외를 던진다")
        void duplicate_phone_throws() {
            given(userRepository.existsByEmail(any())).willReturn(false);
            given(userRepository.existsByPhoneNumberAndDeletedAtIsNull("01011112222")).willReturn(true);

            assertThatThrownBy(() -> authService.signup(signupReq("new@test.com", "01011112222")))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PHONE_NUMBER_DUPLICATED);
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 서비스")
    class Withdraw {
        private User mockUser() {
            SignupRequest req = signupReq("user@test.com", "01099998888");
            return User.of(req, "encodedPw");
        }

        @Test
        @DisplayName("성공 - 회원 탈퇴 성공")
        void success() {
            User user = mockUser();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Test1234!", "encodedPw")).willReturn(true);

            authService.withdraw(1L, new WithdrawRequest("탈퇴합니다.", "Test1234!"));

            assertThat(user.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 제시된 탈퇴 문구를 제대로 입력하지 않으면 예외를 던진다")
        void wrong_confirm_text_throws() {
            User user = mockUser();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    authService.withdraw(1L, new WithdrawRequest("잘못된문구", "Test1234!"))
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("실패 - 저장된 비밀번호와 입력 값이 동일하지 않으면 예외를 던진다")
        void wrong_password_throws() {
            User user = mockUser();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(passwordEncoder.matches(any(), any())).willReturn(false);

            assertThatThrownBy(() ->
                    authService.withdraw(1L, new WithdrawRequest("탈퇴합니다.", "WrongPw!."))
            ).isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PASSWORD_NOT_MATCH);
        }

        @Test
        @DisplayName("실패 - 해당 유저가 이미 탈퇴한 상태면 예외를 던진다")
        void already_deleted_throws() {
            User user = mockUser();
            user.softDelete();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    authService.withdraw(1L, new WithdrawRequest("탈퇴합니다.", "Test1234!"))
            ).isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DELETED_ACCOUNT);
        }
    }
}
