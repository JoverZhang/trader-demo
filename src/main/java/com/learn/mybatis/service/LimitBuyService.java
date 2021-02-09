package com.learn.mybatis.service;

import com.learn.mybatis.core.support.LimitBuyOrderPool;
import com.learn.mybatis.core.support.LimitSellOrderPool;
import com.learn.mybatis.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * 限价买单服务
 * <p>
 * 撮合顺序: 限价卖单 -> 市价卖单 <br/>
 * 最终放置: 限价买单池
 *
 * @author Jover Zhang
 */
@Service
@RequiredArgsConstructor
public class LimitBuyService {

    final LimitBuyOrderPool limitBuyOrderPool;

    final LimitSellOrderPool limitSellOrderPool;

    public List<Order> entry(Order order) {
        List<Order> matchedOrders = new LinkedList<>();

        // 撮合 限价卖单
        matchedOrders.addAll(limitSellOrderPool.match(order));
        if (!matchedOrders.isEmpty()) {
            Optional<BigDecimal> matchedAmount = matchedOrders.stream().map(Order::getAmount).reduce(BigDecimal::add);
            order.setAmount(order.getAmount().subtract(matchedAmount.get()));
            // 当 买单 余额为 0 则直接返回
            if (order.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                return matchedOrders;
            }
        }

        // 将 买单 放入 限价买单池
        limitBuyOrderPool.addOrder(order);

        return matchedOrders;
    }

}
