package com.learn.mybatis.core.support;

import com.learn.mybatis.core.lua.OrderPoolLuaHelper;
import com.learn.mybatis.domain.Order;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author Jover Zhang
 */
public abstract class AbstractOrderPool implements OrderPool {

    private final OrderPoolLuaHelper helper;

    public AbstractOrderPool(StringRedisTemplate redisTemplate) {
        helper = new OrderPoolLuaHelper(getNamespace(), isAscending(), redisTemplate);
    }

    /**
     * 获取队列命名空间
     */
    abstract String getNamespace();

    /**
     * 获取挂单池挂单价格的排序顺序
     * <p>
     * TRUE: 按价格正序 <br/>
     * FALSE: 按价格倒序
     */
    abstract boolean isAscending();

    @Override
    public void add(@Nonnull Order order) {
        helper.add(order);
    }

    @Override
    public void remove(@Nonnull Order order) {
        helper.remove(order);
    }

    @Override
    public List<Order> pop(@Nonnull BigDecimal price, @Nonnull BigDecimal amount) {
        return helper.pop(price, amount);
    }

    @Deprecated
    ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> fetchAllOrders() {
        return helper.fetchOrderPool();
    }

    @Deprecated
    void print() {
        helper.print();
    }

}
