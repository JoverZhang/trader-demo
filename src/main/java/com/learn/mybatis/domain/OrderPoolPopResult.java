package com.learn.mybatis.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * @author Jover Zhang
 */
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderPoolPopResult {

    BigDecimal remainingPrice;

    BigDecimal remainingAmount;

    List<Order> orders;

    static public OrderPoolPopResult build(BigDecimal remainingPrice, BigDecimal remainingAmount, List<Order> orders) {
        return new OrderPoolPopResult(remainingPrice, remainingAmount, orders);
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            return true;
        }
        if (!(obj instanceof OrderPoolPopResult)) {
            return false;
        }
        OrderPoolPopResult o = (OrderPoolPopResult) obj;
        if (!bigDecimalEquals(remainingPrice, o.remainingPrice) ||
                !bigDecimalEquals(remainingAmount, o.remainingAmount)) {
            return false;
        }
        return Objects.equals(orders, o.orders);
    }

    private boolean bigDecimalEquals(@Nullable BigDecimal a, @Nullable BigDecimal b) {
        if (a == null) {
            return b == null;
        }
        return a.compareTo(b) == 0;
    }

}
