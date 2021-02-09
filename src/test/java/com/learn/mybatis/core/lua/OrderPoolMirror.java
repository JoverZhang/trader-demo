package com.learn.mybatis.core.lua;

import com.learn.mybatis.domain.Order;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author Jover Zhang
 */
@NoArgsConstructor
public class OrderPoolMirror {

    ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> orderPool = new ConcurrentSkipListMap<>();

    public OrderPoolMirror(List<Order> orders) {
        orders.forEach(this::addOrder);
    }

    public synchronized void addOrder(Order order) {
        orderPool.compute(order.getPrice(), (k, v) -> {
            if (v == null) {
                v = new LinkedList<>();
            }
            v.add(order);
            return v;
        });
    }

    public synchronized void delOrder(Order order) {
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

    public synchronized List<Order> match(Order outsideOrder, final boolean isAscending) {
        LinkedList<Order> resultSet = new LinkedList<>();
        final BigDecimal outsidePrice = outsideOrder.getPrice();
        BigDecimal outsideAmount = outsideOrder.getAmount();

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
