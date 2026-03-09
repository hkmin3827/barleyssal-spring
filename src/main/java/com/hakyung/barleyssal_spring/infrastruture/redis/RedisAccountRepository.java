package com.hakyung.barleyssal_spring.infrastruture.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hakyung.barleyssal_spring.domain.account.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisAccountRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 유저 계좌 정보 및 보유 주식 통합 캐싱 */
    public void syncAccountToRedis(Account account) {
        // 1. 계좌 기본 정보 (Hash)
        String statusKey = "account:status:" + account.getUserId();
        Map<String, String> statusData = new HashMap<>();
        statusData.put("deposit", account.getDeposit().toString());
        statusData.put("accountId", account.getId().toString());
        statusData.put("principal", account.getPrincipal().toString());
        statusData.put("totalEquity", account.getTotalEquity().toString());
        redisTemplate.opsForHash().putAll(statusKey, statusData);

        // 2. 보유 종목 및 매도 가능 수량 (Hash)
        String holdingKey = "account:holdings:" + account.getUserId();
        redisTemplate.delete(holdingKey); // 초기화 후 재등록


        account.getHoldings().forEach(h -> {
            long sellableQty = h.getTotalQuantity() - h.getBlockedQuantity();
            HoldingCacheDto cacheData = new HoldingCacheDto(
                    h.getAvgPrice(),
                    h.getTotalQuantity(),
                    sellableQty
            );
            try {
                // 객체를 JSON 문자열로 직렬화 (ObjectMapper 주입 필요)
                String jsonValue = objectMapper.writeValueAsString(cacheData);
                redisTemplate.opsForHash().put(holdingKey, h.getStockCode().value(), jsonValue);
            } catch (Exception e) {
                log.error("Redis 직렬화 에러: {}", e.getMessage());
            }
        });
    }
}