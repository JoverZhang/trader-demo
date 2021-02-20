package com.learn.mybatis.core.support;

import lombok.Getter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Jover Zhang
 */
@Component
public class MarketBuyOrderPool extends AbstractOrderPool {

    @Getter
    final String namespace = "MARKET_BUY";

    @Getter
    final boolean isAscending = false;

    public MarketBuyOrderPool(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }

}
