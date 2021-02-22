package com.learn.mybatis.core.lua;

import com.learn.mybatis.core.support.OrderPool;
import com.learn.mybatis.domain.Order;
import com.learn.mybatis.domain.OrderPoolPopResult;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author Jover Zhang
 */
@RequiredArgsConstructor
public class OrderPoolMirror implements OrderPool {

    ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> orderPool = new ConcurrentSkipListMap<>();

    // 通常, `卖单池` 为 true, `买单池` 为 false
    final boolean isAscending;

    public OrderPoolMirror(@Nonnull List<Order> orders, boolean isAscending) {
        this.isAscending = isAscending;
        orders.forEach(this::add);
    }

    @Override
    public synchronized void add(@Nonnull Order order) {
        if (order.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        orderPool.compute(order.getPrice(), (k, v) -> {
            if (v == null) {
                v = new LinkedList<>();
            }
            v.add(order);
            return v;
        });
    }

    @Override
    public synchronized void remove(@Nonnull Order order) {
        orderPool.computeIfPresent(order.getPrice(), (k, v) -> {
            for (Order insideOrder : v) {
                if (insideOrder.getId().equals(order.getId())) {
                    v.remove(insideOrder);
                    break;
                }
            }
            return v.isEmpty() ? null : v;
        });
    }

    @Override
    public OrderPoolPopResult pop(@Nonnull BigDecimal price, @Nonnull BigDecimal amount) {
        return doPop(price, amount, isAscending);
    }

    // TODO
    public OrderPoolPopResult popByPrice(final @Nonnull BigDecimal sumPrice, int precision) {
        List<Order> matchedOrders = new LinkedList<>();
        BigDecimal remainingPrice = sumPrice;

        Order firstOrder;
        while ((firstOrder = peekFirst(isAscending)) != null &&
                remainingPrice.compareTo(BigDecimal.ZERO) > 0) {
            // 以当前挂单的 `price` 可成交的 `amount`
            BigDecimal canTradeAmount = remainingPrice.divide(firstOrder.getPrice(), precision, RoundingMode.DOWN);

            // 当 可交易数量 < 挂单的数量 时
            if (canTradeAmount.compareTo(firstOrder.getAmount()) < 0) {
                matchedOrders.add(Order.builder()
                        .id(firstOrder.getId())
                        .price(firstOrder.getPrice())
                        .amount(canTradeAmount)
                        .build());
                // TODO: 逻辑上不会为 false
                Assert.isTrue(
                        compareAndUpdateFirst(firstOrder, Order.builder()
                                .id(firstOrder.getId())
                                .price(firstOrder.getPrice())
                                .amount(firstOrder.getAmount().subtract(canTradeAmount))
                                .build(), isAscending)
                );
                remainingPrice = BigDecimal.ZERO;
            }
            // 当 可交易数量 >= 挂单的数量 时
            else {
                matchedOrders.add(firstOrder);
                // TODO: 逻辑上不会为 false
                Assert.isTrue(delFirstByExpect(firstOrder, isAscending));
                canTradeAmount = canTradeAmount.subtract(firstOrder.getAmount());
                remainingPrice = canTradeAmount.multiply(firstOrder.getPrice());
            }
        }

        return OrderPoolPopResult.build(remainingPrice, null, matchedOrders);
    }

    public OrderPoolPopResult popByAmount(@Nonnull BigDecimal amount) {
        return doPop(new BigDecimal("0"), amount, false);
    }

    synchronized OrderPoolPopResult doPop(final @Nonnull BigDecimal price, final @Nonnull BigDecimal amount,
                                          boolean isAscending) {
        LinkedList<Order> matchedOrders = new LinkedList<>();
        BigDecimal remainingAmount = amount;

        Order firstOrder;
        while ((firstOrder = peekFirst(isAscending)) != null &&
                validatePriceForPop(price, firstOrder.getPrice(), isAscending) &&
                remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            // 当 剩余数量 < 挂单的数量 时
            // 挂单的数量 -= 剩余数量, 剩余数量 = 0
            if (remainingAmount.compareTo(firstOrder.getAmount()) < 0) {
                matchedOrders.add(Order.builder()
                        .id(firstOrder.getId())
                        .price(firstOrder.getPrice())
                        .amount(remainingAmount)
                        .build());
                // TODO: 逻辑上不会为 false
                Assert.isTrue(
                        compareAndUpdateFirst(firstOrder, Order.builder()
                                .id(firstOrder.getId())
                                .price(firstOrder.getPrice())
                                .amount(firstOrder.getAmount().subtract(remainingAmount))
                                .build(), isAscending)
                );
                remainingAmount = BigDecimal.ZERO;
            }
            // 当 剩余数量 >= 挂单的数量 时
            // 删除第一个挂单, 剩余数量 -= 挂单的数量
            else {
                matchedOrders.add(firstOrder);
                // TODO: 逻辑上不会为 false
                Assert.isTrue(delFirstByExpect(firstOrder, isAscending));
                remainingAmount = remainingAmount.subtract(firstOrder.getAmount());
            }
        }

        return OrderPoolPopResult.build(price, remainingAmount, matchedOrders);
    }

    /**
     * 判断 `sourcePrice` 是否满足 pop `targetPrice` 的条件
     * <p>正常情况下, 当 `isAscending` 为 true 时, 当前 orderPool 为 `卖单池`, 所以 `sourcePrice` 需要大于等于 `targetPrice`,
     * 反之亦然
     */
    private boolean validatePriceForPop(@Nonnull BigDecimal sourcePrice, @Nonnull BigDecimal targetPrice,
                                        boolean isAscending) {
        return isAscending ?
                sourcePrice.compareTo(targetPrice) >= 0 :
                sourcePrice.compareTo(targetPrice) <= 0;
    }

    @Nullable
    private synchronized Order peekFirst(boolean isAscending) {
        Entry<BigDecimal, LinkedList<Order>> firstEntry = peekFirstEntry(isAscending);
        if (firstEntry == null) {
            return null;
        }
        return firstEntry.getValue().getFirst();
    }

    @Nullable
    private synchronized Entry<BigDecimal, LinkedList<Order>> peekFirstEntry(boolean isAscending) {
        Entry<BigDecimal, LinkedList<Order>> entry = isAscending ? orderPool.firstEntry() : orderPool.lastEntry();
        if (entry == null) {
            return null;
        }
        Assert.notEmpty(entry.getValue());
        return entry;
    }

    private synchronized boolean delFirstByExpect(@Nonnull Order expect, boolean isAscending) {
        Order actual = peekFirst(isAscending);
        if (!expect.equals(actual)) {
            return false;
        }
        Entry<BigDecimal, LinkedList<Order>> firstEntry = peekFirstEntry(isAscending);
        if (firstEntry == null) {
            return false;
        }
        if (firstEntry.getValue().size() == 1) {
            Assert.isTrue(orderPool.remove(firstEntry.getKey(), firstEntry.getValue()));
        } else {
            Assert.isTrue(firstEntry.getValue().remove(actual));
        }
        return true;
    }

    private synchronized boolean compareAndUpdateFirst(@Nonnull Order expect, @Nonnull Order target,
                                                       boolean isAscending) {
        Assert.isTrue(expect.getId().equals(target.getId()));
        Order actual = peekFirst(isAscending);
        if (!expect.equals(actual)) {
            return false;
        }
        actual.setPrice(target.getPrice());
        actual.setAmount(target.getAmount());
        return true;
    }

    @Override
    public String toString() {
        return orderPool.toString();
    }

}
