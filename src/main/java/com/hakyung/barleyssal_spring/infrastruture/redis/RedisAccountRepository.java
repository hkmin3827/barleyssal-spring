package com.hakyung.barleyssal_spring.infrastruture.redis;

import com.hakyung.barleyssal_spring.domain.account.Account;
import com.hakyung.barleyssal_spring.domain.holding.Holding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
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

            validateAccount(account);

            updateAccountStatus(account);

            Map<String, String> holdingsMap = new HashMap<>();
            Map<String, String> metaMap = new HashMap<>();


            for (Holding h : account.getHoldings()) {
                long sellable = h.getTotalQuantity() - h.getBlockedQuantity();
                if (sellable > 0) {
                    holdingsMap.put(h.getStockCode().value(), String.valueOf(sellable));
                    metaMap.put(h.getStockCode().value(), serializeMeta(h));
                }
            }

            Long userId = account.getUserId();

            atomicSync(userId, "account:holdings:", holdingsMap);
            atomicSync(userId, "account:holdings:meta:", metaMap);

            log.debug("Account synced to Redis atomically : userId={}", account.getUserId());
        } catch (Exception e) {
            log.error("syncAccountToRedis failed : userId={} : {}", account.getUserId(), e.getMessage());
        }
    }

    private void validateAccount(Account account) {
        Objects.requireNonNull(account.getId(), "accountId는 절대 null일 수 없습니다.");
        Objects.requireNonNull(account.getDeposit(), "deposit은 절대 null일 수 없습니다.");
        Objects.requireNonNull(account.getPrincipal(), "principal은 절대 null일 수 없습니다.");
    }
    private void updateAccountStatus(Account account) {
        String statusKey = "account:status:" + account.getUserId();
        Map<String, String> statusData = new HashMap<>();
        statusData.put("deposit", account.getDeposit().toString());
        statusData.put("accountId", account.getId().toString());
        statusData.put("userName", account.getUserName());
        statusData.put("principal", account.getPrincipal().toString());

        redisTemplate.opsForHash().putAll(statusKey, statusData);
    }

    private String serializeMeta(Holding h) {
        try {
            Map<String, String> meta = new HashMap<>();
            meta.put("avgPrice", h.getAvgPrice().toString());
            meta.put("totalQuantity", h.getTotalQuantity().toString());
            meta.put("blockedQuantity", h.getBlockedQuantity() != null ? h.getBlockedQuantity().toString() : "0");
            return objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            log.warn("Failed to serialize holding meta for {}", h.getStockCode().value(), e);
            return "{}";
        }
    }

    private void atomicSync(Long userId, String keyPrefix, Map<String, String> dataMap) {
        String realKey = keyPrefix + userId;
        String tempKey = realKey + ":temp";

        if (dataMap.isEmpty()) {
            redisTemplate.delete(realKey);
            return;
        }

        redisTemplate.opsForHash().putAll(tempKey, dataMap);
        redisTemplate.rename(tempKey, realKey);
    }
}