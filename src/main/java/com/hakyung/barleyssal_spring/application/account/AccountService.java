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
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final StringRedisTemplate redisTemplate;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public AccountResponse getOrCreateAccount(Long userId) {
        return accountRepository.findByUserId(userId)
                .map(AccountResponse::from)
                .orElseGet(() -> {
                    String newAccountNumber = accountNumberGenerator.generate();
                    var account = Account.create(userId, Money.of(0L), newAccountNumber);
                    return AccountResponse.from(accountRepository.save(account));
                });
    }

    @Transactional
    public AccountResponse setPrincipal(Long userId, SetPrincipalRequest req) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.resetPrincipal(Money.of(req.principal()));
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> getHoldings(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_NOT_FOUND));
        return account.getHoldings().stream()
                .map(h -> {
                    String priceKey = "market:price:" + h.getStockCode().value();
                    String priceStr = redisTemplate.opsForValue().get(priceKey);
                    BigDecimal currentPrice = priceStr != null ? new BigDecimal(priceStr) : null;
                    return HoldingResponse.from(h);
                })
                .toList();
    }
}
