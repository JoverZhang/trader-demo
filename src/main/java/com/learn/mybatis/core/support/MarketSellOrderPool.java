package com.learn.mybatis.core.support;

import lombok.Getter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Jover Zhang
 */
@Component
public class MarketSellOrderPool extends AbstractOrderPool {

    @Getter
    final String namespace = "MARKET_SELL";

    @Getter
    final boolean isAscending = true;

    public MarketSellOrderPool(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }

}
