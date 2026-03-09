package com.hakyung.barleyssal_spring.application.account;

import com.hakyung.barleyssal_spring.application.account.dto.AccountResponse;
import com.hakyung.barleyssal_spring.application.account.dto.HoldingResponse;
import com.hakyung.barleyssal_spring.application.account.dto.SetPrincipalRequest;
import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.account.AccountNumberGenerator;
import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.global.constant.ErrorCode;
import com.hakyung.barleyssal_spring.global.exception.CustomException;
import com.hakyung.barleyssal_spring.infrastrutrue.redis.RedisAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final StringRedisTemplate redisTemplate;
    private final AccountNumberGenerator accountNumberGenerator;
    private final RedisAccountRepository redisAccountRepository;

    @Transactional
    public AccountResponse getOrCreateAccount(Long userId) {
        Account account =  accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    String newAccountNumber = accountNumberGenerator.generate();
                    var newAccount = Account.create(userId, Money.of(0L), newAccountNumber);
                    return accountRepository.save(newAccount);
                });

        redisAccountRepository.syncAccountToRedis(account);
        return AccountResponse.from(account);
    }


    @Transactional
    public AccountResponse setPrincipal(Long userId, SetPrincipalRequest req) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.resetPrincipal(Money.of(req.principal()));

        redisAccountRepository.syncAccountToRedis(account);

        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> getHoldings(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        return account.getHoldings().stream()
                .map(HoldingResponse::from)
                .toList();
    }

    public void syncAccountToRedis(Long userId) {
        Account account = accountRepository.findByUserIdWithHoldings(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));

        String statusKey = "account:status:" + userId;
        Map<String, String> data = new HashMap<>();
        data.put("deposit", account.getDeposit().toString());
        data.put("principal", account.getPrincipal().toString());
        data.put("totalEquity", account.getTotalEquity().toString());
        redisTemplate.opsForHash().putAll(statusKey, data);

        String holdingKey = "account:holdings:" + userId;
        redisTemplate.delete(holdingKey);
        account.getHoldings().forEach(h -> {
            long sellableQty = h.getTotalQuantity() - h.getBlockedQuantity();
            redisTemplate.opsForHash().put(holdingKey, h.getStockCode().value(), String.valueOf(sellableQty));
        });
    }
}
