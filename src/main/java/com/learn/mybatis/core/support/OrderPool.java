package com.learn.mybatis.core.support;

import com.learn.mybatis.domain.Order;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Jover Zhang
 */
public interface OrderPool {

    /**
     * 将 `新挂单` 加入到挂单池
     *
     * @param order 新挂单
     */
    void add(@Nonnull Order order);

    /**
     * 从挂单池中删除 `挂单`
     *
     * @param order 挂单
     */
    void remove(@Nonnull Order order);

    /**
     * 在 `price` 限制的价格区间内, 按顺序 (`isAscending`) 弹出`交易数量`总和为 `amount` 的挂单.
     *
     * @param price  价格
     * @param amount 交易数量
     * @return 符合弹出条件的 `挂单` 列表
     */
    List<Order> pop(@Nonnull BigDecimal price, @Nonnull BigDecimal amount);

}
