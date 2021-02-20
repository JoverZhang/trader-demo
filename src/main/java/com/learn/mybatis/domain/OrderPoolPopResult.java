package com.learn.mybatis.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Jover Zhang
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderPoolPopResult {

    BigDecimal remainingPrice;

    BigDecimal remainingAmount;

    List<Order> orders;

    static public OrderPoolPopResult build(BigDecimal remainingPrice, BigDecimal remainingAmount, List<Order> orders) {
        return new OrderPoolPopResult(remainingPrice, remainingAmount, orders);
    }

}
