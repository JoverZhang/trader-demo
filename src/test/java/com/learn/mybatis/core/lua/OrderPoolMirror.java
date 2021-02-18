package com.learn.mybatis.core.lua;

import com.learn.mybatis.core.support.OrderPool;
import com.learn.mybatis.domain.Order;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Iterator;
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
    public List<Order> pop(@Nonnull BigDecimal price, @Nonnull BigDecimal amount) {
        return doPop(price, amount, isAscending);
    }

    synchronized List<Order> doPop(final @Nonnull BigDecimal outsidePrice, @Nonnull BigDecimal outsideAmount,
                                   boolean isAscending) {
        LinkedList<Order> resultSet = new LinkedList<>();
        boolean isCompleted = false;

        Iterator<Entry<BigDecimal, LinkedList<Order>>> orderPoolIterator =
                new Iterator<Entry<BigDecimal, LinkedList<Order>>>() {
                    Entry<BigDecimal, LinkedList<Order>> lastReturn;

                    @Override
                    public boolean hasNext() {
                        lastReturn = isAscending ? orderPool.firstEntry() : orderPool.lastEntry();
                        if (lastReturn == null) {
                            return false;
                        }
                        BigDecimal nextPrice = lastReturn.getValue().getFirst().getPrice();
                        if (isAscending) {
                            return outsidePrice.compareTo(nextPrice) >= 0;
                        }
                        return outsidePrice.compareTo(nextPrice) <= 0;
                    }

                    @Override
                    public Entry<BigDecimal, LinkedList<Order>> next() {
                        return lastReturn;
                    }

                    @Override
                    public void remove() {
                        orderPool.remove(lastReturn.getKey(), lastReturn.getValue());
                    }
                };

        while (!isCompleted && orderPoolIterator.hasNext()) {
            Entry<BigDecimal, LinkedList<Order>> entry = orderPoolIterator.next();
            LinkedList<Order> insideOrders = entry.getValue();

            Iterator<Order> insideOrderIterator = insideOrders.iterator();
            while (insideOrderIterator.hasNext()) {
                Order insideOrder = insideOrderIterator.next();
                BigDecimal insideAmount = insideOrder.getAmount();
                if (outsideAmount.compareTo(insideAmount) < 0) {
                    resultSet.add(Order.builder()
                            .id(insideOrder.getId())
                            .price(insideOrder.getPrice())
                            .amount(outsideAmount)
                            .build());
                    insideOrder.setAmount(insideAmount.subtract(outsideAmount));
                    outsideAmount = BigDecimal.ZERO;
                }
                // 需要删除 inside order
                else {
                    resultSet.add(Order.builder()
                            .id(insideOrder.getId())
                            .price(insideOrder.getPrice())
                            .amount(insideAmount)
                            .build());
                    insideOrderIterator.remove();
                    outsideAmount = outsideAmount.subtract(insideAmount);
                }

                if (outsideAmount.compareTo(BigDecimal.ZERO) == 0) {
                    isCompleted = true;
                    break;
                }
            }

            if (insideOrders.isEmpty()) {
                orderPoolIterator.remove();
            }
        }

        return resultSet;
    }

    @Override
    public String toString() {
        return orderPool.toString();
    }

}
