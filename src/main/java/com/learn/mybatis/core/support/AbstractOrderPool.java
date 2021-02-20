package com.learn.mybatis.core.support;

import com.learn.mybatis.core.lua.OrderPoolLuaHelper;
import com.learn.mybatis.domain.Order;
import com.learn.mybatis.domain.OrderPoolPopResult;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.LinkedList;
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
    public OrderPoolPopResult pop(@Nonnull BigDecimal price, @Nonnull BigDecimal amount) {
        return helper.pop(price, amount);
    }

    /**
     * 仅用于 `卖单池`
     * <p>根据传入 `price` 动态计算 `amount`, 并弹出总和为 `amount` 的挂单.
     * <p><pre>
     * <b>方法传入参数:</b>
     * `sum_price` = 30 (在运行时动态计算 `sum_amount`)
     *
     * <b>卖单池内状态:</b>
     * price    amount
     * 3        20
     * 2        10
     *
     * <b>计算顺序:</b>
     * 30 / 2  = 15 (amount)     // 计算基于价格为 2 时可消费的 `amount`
     * 15 - 10 = 5  (sum_amount) // 消费 10 个价格为 2 的挂单, 计算出剩余 5 的 `sum_amount`
     * 5  * 2  = 10 (sum_price)  // 将 `sum_amount` 乘回当前 `price` 得出剩余 `sum_price`
     *
     * // 此时 `price` 为 2 的挂单已消耗殆尽, 但 `sum_price` 仍然有剩余
     *
     * 10 / 3 = 3.333...(amount) // 计算基于价格为 3 时可消费的 `amount`
     * 3.333... - 20 = -16.666...// 期望消费 20 个价格为 3 的挂单, 但仅能成功消费 `3.333...` 的 `amount`
     *
     * <b>处理结果:</b>
     * <b>方法传入参数:</b>
     * sum_price = 0
     *
     * <b>卖单池内状态:</b>
     * price    amount
     * 3        16.666...
     * </pre>
     *
     * @param sumPrice 总交易价格
     * @return 符合弹出条件的 `挂单` 列表
     */
    public OrderPoolPopResult popByPrice(@Nonnull BigDecimal sumPrice) {
        throw new UnsupportedOperationException();
    }

    /**
     * 仅用于 `买单池`
     * <p>
     * 弹出总和为 `amount` 的挂单.
     * <p><pre>
     * <b>方法传入参数:</b>
     * `sum_amount` = 20
     *
     * <b>买单池内状态:</b>
     * price    amount
     * 2        10
     * 1        20
     *
     * <b>处理顺序:</b> (忽略买单池中的 `price`)
     * 20 - 10 = 10  // 直接减去挂单中的 `amount = 10`, 并剩余 10 的 `sum_amount`
     * 10 - 20 = -10 // 直接减去挂单中的 `amount = 20`, 挂单依然剩余 10 的 `amount`
     *
     * <b>处理结果:</b>
     * sum_amount = 0
     *
     * <b>买单池内状态:</b>
     * price    amount
     * 1        10
     * </pre>
     *
     * @param sumAmount 总交易数量
     * @return 符合弹出条件的 `挂单` 列表
     */
    public OrderPoolPopResult popByAmount(@Nonnull BigDecimal sumAmount) {
        return helper.popByAmount(sumAmount);
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
