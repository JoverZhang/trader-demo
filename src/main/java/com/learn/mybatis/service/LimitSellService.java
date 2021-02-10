package com.learn.mybatis.service;

import com.learn.mybatis.core.support.LimitBuyOrderPool;
import com.learn.mybatis.core.support.LimitSellOrderPool;
import com.learn.mybatis.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

/**
 * 限价买单服务
 * <p>
 * 撮合顺序: 限价买单 -> 市价买单 <br/>
 * 最终放置: 限价卖单池
 *
 * @author Jover Zhang
 */
@Service
@RequiredArgsConstructor
public class LimitSellService {

    final LimitSellOrderPool limitSellOrderPool;

    final LimitBuyOrderPool limitBuyOrderPool;

    public List<Order> entry(Order order) {
        List<Order> matchedOrders = new LinkedList<>();

        // 撮合 限价买单
        matchedOrders.addAll(limitBuyOrderPool.match(order));
        // 匹配有效则更新 外单 余额
        if (!matchedOrders.isEmpty()) {
            BigDecimal sumAmount = matchedOrders.stream().map(Order::getAmount).reduce(BigDecimal::add).get();
            order.setAmount(order.getAmount().subtract(sumAmount));
            // 当 买单 余额为 0 则直接返回
            if (order.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                return matchedOrders;
            }
        }

        // 将 卖单 放入 限价卖单池
        limitSellOrderPool.addOrder(order);

        return matchedOrders;
    }

}
