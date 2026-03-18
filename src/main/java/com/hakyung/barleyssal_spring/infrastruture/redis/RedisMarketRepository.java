package com.hakyung.barleyssal_spring.infrastruture.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisMarketRepository {

    private final StringRedisTemplate redisTemplate;

    private String getInfoKey(String stockCode) {
        return "market:info:" + stockCode;
    }

    public Double getCurrentPrice(String stockCode) {
        Object priceObj = redisTemplate.opsForHash().get(getInfoKey(stockCode), "price");
        return parseDouble(priceObj);
    }

    public Double getHighPrice(String stockCode) {
        Object highPriceObj = redisTemplate.opsForHash().get(getInfoKey(stockCode), "stckHgpr");
        return parseDouble(highPriceObj);
    }

    public String getMarketOperationCode(String stockCode) {
        Object mKopCode = redisTemplate.opsForHash().get(getInfoKey(stockCode), "mKopCode");
        return mKopCode != null ? mKopCode.toString() : null;
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("RedisMarketRepository parse error. value: {}", value);
            return null;
        }
    }
}