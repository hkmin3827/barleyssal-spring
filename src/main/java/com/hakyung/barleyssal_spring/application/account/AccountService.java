package com.hakyung.barleyssal_spring.application.account;

import com.hakyung.barleyssal_spring.application.account.dto.AccountResponse;
import com.hakyung.barleyssal_spring.application.account.dto.HoldingResponse;
import com.hakyung.barleyssal_spring.application.account.dto.SetPrincipalRequest;
import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountNumberGenerator;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.order.Order;
import com.hakyung.barleyssal_spring.domain.order.OrderRepository;
import com.hakyung.barleyssal_spring.domain.order.OrderStatus;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.AccountNotFoundException;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final RedisAccountRepository redisAccountRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public AccountResponse getOrCreateAccount(Long userId, String userName) {
        Account account =  accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    String newAccountNumber = accountNumberGenerator.generate();
                    var newAccount = Account.create(userId, userName, Money.of(0L), newAccountNumber);
                    accountRepository.save(newAccount);
                    redisAccountRepository.syncAccountToRedis(newAccount);
                    return newAccount;
                });

        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse setPrincipal(Long userId, SetPrincipalRequest req) {
        if (req.principal().remainder(BigDecimal.TEN).compareTo(BigDecimal.ZERO) != 0) {
            throw new CustomException(ErrorCode.INVALID_PRINCIPAL_UNIT);
        }
        Account account = accountRepository.findByUserIdWithLock(userId)
                .orElseThrow(AccountNotFoundException::new);

        List<Order> activeOrders = orderRepository
                .findByAccountIdAndOrderStatusIn(
                        account.getId(),
                        List.of(OrderStatus.PENDING, OrderStatus.SUBMITTED)
                );

        for (Order order : activeOrders) {
            order.cancel();
        }
        orderRepository.saveAll(activeOrders);

        account.resetPrincipal(Money.of(req.principal()));

        redisAccountRepository.syncAccountToRedis(account);

        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> getHoldings(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(AccountNotFoundException::new);

        return account.getHoldings().stream()
                .map(HoldingResponse::from)
                .toList();
    }
}
