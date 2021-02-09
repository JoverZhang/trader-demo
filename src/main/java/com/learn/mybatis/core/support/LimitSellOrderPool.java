package com.learn.mybatis.core.support;

import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * @author Jover Zhang
 */
@Component
public class LimitSellOrderPool extends AbstractOrderPool {

    @Getter
    final String namespace = "LIMIT_SELL";

    @Getter
    final boolean isAscending = true;

}
