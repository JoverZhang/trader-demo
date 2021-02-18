package com.learn.mybatis.core.support;

import lombok.Getter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Jover Zhang
 */
@Component
public class LimitBuyOrderPool extends AbstractOrderPool {

    @Getter
    final String namespace = "LIMIT_BUY";

    @Getter
    final boolean isAscending = false;

    public LimitBuyOrderPool(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }

}
