package com.learn.mybatis.common;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * TODO
 *
 * @author Jover Zhang
 */
@AllArgsConstructor
@RequiredArgsConstructor
public class Randomizer {

    final String minimum;

    final String maximum;

    int precision = 0;

    public int getInt() {
        return new Integer(getString());
    }

    public BigDecimal getBigDecimal() {
        return new BigDecimal(getString());
    }

    public String getString() {
        BigDecimal min = new BigDecimal(minimum);
        BigDecimal max = new BigDecimal(maximum);
        return min.add(BigDecimal.valueOf(Math.random()).multiply(max.subtract(min)))
                .setScale(precision, RoundingMode.DOWN).toPlainString();
    }

}
