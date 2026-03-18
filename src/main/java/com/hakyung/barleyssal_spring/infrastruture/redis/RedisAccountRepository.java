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
import java.util.Objects;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisAccountRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void syncAccountToRedis(Account account) {
        try {
            Objects.requireNonNull(account.getId(), "accountId는 절대 null일 수 없습니다.");
            Objects.requireNonNull(account.getDeposit(), "deposit은 절대 null일 수 없습니다.");
            Objects.requireNonNull(account.getPrincipal(), "principal은 절대 null일 수 없습니다.");

            String statusKey = "account:status:" + account.getUserId();
            Map<String, String> statusData = new HashMap<>();
            statusData.put("deposit", account.getDeposit().toString());
            statusData.put("accountId", account.getId().toString());
            statusData.put("userName", account.getUserName());
            statusData.put("principal", account.getPrincipal().toString());
            redisTemplate.opsForHash().putAll(statusKey, statusData);

            String holdingKey = "account:holdings:" + account.getUserId();
            redisTemplate.delete(holdingKey);

            account.getHoldings().forEach(h -> {
                long sellable = h.getTotalQuantity() - h.getBlockedQuantity();
                if (sellable > 0) {
                    redisTemplate.opsForHash().put(holdingKey,
                            h.getStockCode().value(), String.valueOf(sellable));
                }

            });

            String metaKey = "account:holdings:meta:" + account.getUserId();
            redisTemplate.delete(metaKey);

            for (Holding h : account.getHoldings()) {
                if (h.getTotalQuantity() <= 0) continue;
                try {
                    Objects.requireNonNull(h.getAvgPrice(), "h.getAvgPrice는 절대 null일 수 없습니다.");
                    Objects.requireNonNull(h.getTotalQuantity(), "h.getTotalQuantity는 절대 null일 수 없습니다.");

                    Map<String, Object> meta = new HashMap<>();
                    meta.put("avgPrice",         h.getAvgPrice().toString());
                    meta.put("totalQuantity",     h.getTotalQuantity().toString());
                    meta.put("blockedQuantity",   h.getBlockedQuantity() != null
                            ? h.getBlockedQuantity().toString() : "0");
                    String metaJson = objectMapper.writeValueAsString(meta);
                    redisTemplate.opsForHash().put(metaKey, h.getStockCode().value(), metaJson);
                } catch (Exception e) {
                    log.warn("Failed to serialize holding meta for {}", h.getStockCode().value(), e);
                }
            }

            log.debug("Account synced to Redis: userId={}", account.getUserId());
        } catch (Exception e) {
            log.error("syncAccountToRedis failed : {}", e.getMessage());
        }
    }
}