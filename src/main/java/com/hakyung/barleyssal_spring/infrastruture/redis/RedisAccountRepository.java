package com.hakyung.barleyssal_spring.infrastruture.redis;

import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.holding.Holding;
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
        try {
            String statusKey = "account:status:" + account.getUserId();
            Map<String, String> statusData = new HashMap<>();
            statusData.put("deposit", account.getDeposit().toString());
            statusData.put("accountId", account.getId().toString());
            statusData.put("principal", account.getPrincipal().toString());
            redisTemplate.opsForHash().putAll(statusKey, statusData);

            // 2. 보유 종목 및 매도 가능 수량 (Hash)
            String holdingKey = "account:holdings:" + account.getUserId();
            redisTemplate.delete(holdingKey); // 초기화 후 재등록


            account.getHoldings().forEach(h -> {
                long sellable = h.getTotalQuantity() - h.getBlockedQuantity();
                if (sellable > 0) {
                    redisTemplate.opsForHash().put(holdingKey,
                            h.getStockCode().value(), String.valueOf(sellable));
                }

            });

            String userIdKey = "account:userId:" + account.getId();
            redisTemplate.opsForValue().set(userIdKey, String.valueOf(account.getUserId()));

            // ── [추가 2] holdings 메타 (avgPrice + totalQuantity) ────────
            // Node.js PnL 실시간 계산에 사용
            String metaKey = "account:holdings:meta:" + account.getUserId();
            redisTemplate.delete(metaKey);

            for (Holding h : account.getHoldings()) {
                if (h.getTotalQuantity() <= 0) continue;
                try {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("avgPrice",         h.getAvgPrice().toString());
                    meta.put("totalQuantity",     String.valueOf(h.getTotalQuantity()));
                    meta.put("blockedQuantity",   String.valueOf(h.getBlockedQuantity() != null
                            ? h.getBlockedQuantity() : 0));
                    String metaJson = objectMapper.writeValueAsString(meta);
                    redisTemplate.opsForHash().put(metaKey, h.getStockCode().value(), metaJson);
                } catch (Exception e) {
                    log.warn("Failed to serialize holding meta for {}", h.getStockCode().value(), e);
                }
            }

            log.debug("Account synced to Redis: userId={}", account.getUserId());
        } catch (Exception e) {
            log.error("syncAccountToRedis failed");
        }
    }
}