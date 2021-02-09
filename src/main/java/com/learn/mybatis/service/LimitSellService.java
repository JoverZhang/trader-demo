package com.learn.mybatis.service;

import com.learn.mybatis.core.support.LimitBuyOrderPool;
import com.learn.mybatis.core.support.LimitSellOrderPool;
import com.learn.mybatis.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * @author Jover Zhang
 */
@Service
@RequiredArgsConstructor
public class LimitSellService {

    final LimitSellOrderPool limitSellOrderPool;

    final LimitBuyOrderPool limitBuyOrderPool;

    public void entry(Order order) {
        List<Order> matched = limitBuyOrderPool.match(order);

        Optional<BigDecimal> matchedAmount = matched.stream().map(Order::getAmount).reduce(BigDecimal::add);
        System.out.println("matchedAmount = " + matchedAmount);
    }

}
