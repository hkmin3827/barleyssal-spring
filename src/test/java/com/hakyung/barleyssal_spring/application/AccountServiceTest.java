package com.hakyung.barleyssal_spring.application;

import com.hakyung.barleyssal_spring.application.account.AccountService;
import com.hakyung.barleyssal_spring.application.account.dto.SetPrincipalRequest;
import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountNumberGenerator;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.global.exception.AccountNotFoundException;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService 단위 테스트")
class AccountServiceTest {

    @InjectMocks AccountService accountService;
    @Mock AccountRepository accountRepository;
    @Mock AccountNumberGenerator accountNumberGenerator;
    @Mock RedisAccountRepository redisAccountRepository;

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.create(1L, "테스터", Money.of(0L), "ACC-001");
    }

    @Nested
    @DisplayName("계좌 생성/조회")
    class GetOrCreate {
        @Test
        @DisplayName("성공 - 기존 계좌가 있으면 조회 결과를 반환한다")
        void returns_existing_account() {
            given(accountRepository.findByUserId(1L)).willReturn(Optional.of(account));

            var result = accountService.getOrCreateAccount(1L, "테스터");

            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(1L);
            then(accountRepository).should(never()).save(any());
            then(redisAccountRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("성공 - 계좌 정보가 없으면 신규 계좌를 생성한다")
        void creates_new_account_when_not_exists() {
            given(accountRepository.findByUserId(1L)).willReturn(Optional.empty());
            given(accountNumberGenerator.generate()).willReturn("ACC-NEW");
            given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            var result = accountService.getOrCreateAccount(1L, "신규");

            assertThat(result).isNotNull();
            then(accountRepository).should().save(any(Account.class));
            then(redisAccountRepository).should().syncAccountToRedis(any());
        }
    }

    @Nested
    @DisplayName("계좌 원금 관리")
    class SetPrincipal {
        @Test
        @DisplayName("성공 - 10원 단위 이상으로 원금 설정 시 원금과 예수금 갱신된다")
        void success_resets_principal_and_deposit() {
            given(accountRepository.findByUserId(1L)).willReturn(Optional.of(account));
            given(accountRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            var req = new SetPrincipalRequest(new BigDecimal("5000000"));
            var result = accountService.setPrincipal(1L, req);

            assertThat(result.principal()).isEqualByComparingTo("5000000.00");
            assertThat(result.deposit()).isEqualByComparingTo("5000000.00");
        }

        @Test
        @DisplayName("실패 - 원금은 10원 단위가 아니면 예외를 던진다")
        void not_10_unit_throws() {
            var req = new SetPrincipalRequest(new BigDecimal("5000001"));

            assertThatThrownBy(() -> accountService.setPrincipal(1L, req))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("실패 - 계좌 조회 실패")
        void account_not_found_throws() {
            given(accountRepository.findByUserId(99L)).willReturn(Optional.empty());

            var req = new SetPrincipalRequest(new BigDecimal("1000000"));

            assertThatThrownBy(() -> accountService.setPrincipal(99L, req))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }
}
