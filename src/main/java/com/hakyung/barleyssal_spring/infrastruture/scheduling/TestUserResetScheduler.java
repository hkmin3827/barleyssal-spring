package com.hakyung.barleyssal_spring.infrastruture.scheduling;

import com.hakyung.barleyssal_spring.domain.account.AccountRepository;
import com.hakyung.barleyssal_spring.domain.common.vo.Money;
import com.hakyung.barleyssal_spring.domain.order.OrderRepository;
import com.hakyung.barleyssal_spring.domain.user.UserRepository;
import com.hakyung.barleyssal_spring.domain.watchlist.WatchlistRepository;
import com.hakyung.barleyssal_spring.infrastruture.redis.RedisAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestUserResetScheduler {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final RedisAccountRepository redisAccountRepository;
    private final WatchlistRepository watchlistRepository;
    private final OrderRepository orderRepository;

    @Transactional
    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
    public void clearUserScheduler(){
        log.info("Clearing Test User Scheduler ...");
        String testUserEmail = "test@stock.com";

        userRepository.findByEmail(testUserEmail).ifPresent(user -> {
            Long userId = user.getId();

            accountRepository.findByUserId(userId).ifPresent(account -> {
                orderRepository.deleteByAccountId(account.getId());
                account.resetPrincipal(Money.ZERO);
                accountRepository.save(account);

                redisAccountRepository.syncAccountToRedis(account);
            });

            watchlistRepository.deleteByUserId(userId);
        });
    }
}
