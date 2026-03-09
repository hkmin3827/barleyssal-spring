package com.hakyung.barleyssal_spring.infrastrutrue.redis;

import com.hakyung.barleyssal_spring.domain.account.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RedisAccountRepository {

    private final StringRedisTemplate redisTemplate;

    /** 유저 계좌 정보 및 보유 주식 통합 캐싱 */
    public void syncAccountToRedis(Account account) {
        // 1. 계좌 기본 정보 (Hash)
        String statusKey = "account:status:" + account.getUserId();
        Map<String, String> statusData = new HashMap<>();
        statusData.put("deposit", account.getDeposit().toString());
        statusData.put("principal", account.getPrincipal().toString());
        statusData.put("totalEquity", account.getTotalEquity().toString());
        redisTemplate.opsForHash().putAll(statusKey, statusData);

        // 2. 보유 종목 및 매도 가능 수량 (Hash)
        String holdingKey = "account:holdings:" + account.getUserId();
        redisTemplate.delete(holdingKey); // 초기화 후 재등록

        account.getHoldings().forEach(h -> {
            long sellableQty = h.getTotalQuantity() - h.getBlockedQuantity();
            if (sellableQty > 0) {
                redisTemplate.opsForHash().put(holdingKey, h.getStockCode().value(), String.valueOf(sellableQty));
            }
        });
    }
}