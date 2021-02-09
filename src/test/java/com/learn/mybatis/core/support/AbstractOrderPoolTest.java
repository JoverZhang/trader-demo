package com.learn.mybatis.core.support;

import com.learn.mybatis.MainTest;
import com.learn.mybatis.core.lua.OrderPoolLuaHelper;
import com.learn.mybatis.domain.Order;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest(classes = MainTest.class)
class AbstractOrderPoolTest {

    @Autowired
    OrderPoolLuaHelper helper;

    AbstractOrderPool absOrderPool;

    @BeforeEach
    public void before() {
        absOrderPool = new AbstractOrderPool() {
            @Getter
            final String namespace = "TEST";

            @Getter
            final boolean isAscending = true;
        };
        absOrderPool.setHelper(helper);
    }

    @Test
    void testAddOrder() {
        absOrderPool.addOrder(new Order("ls1", new BigDecimal("1"), new BigDecimal("10")));
        absOrderPool.addOrder(new Order("ls2", new BigDecimal("2"), new BigDecimal("10")));
        absOrderPool.addOrder(new Order("ls3", new BigDecimal("3"), new BigDecimal("10")));
        absOrderPool.addOrder(new Order("ls4", new BigDecimal("2"), new BigDecimal("10")));
    }

    @Test
    void testDelOrder() {
        absOrderPool.delOrder(new Order("ls1", new BigDecimal("1"), new BigDecimal("10")));
        absOrderPool.delOrder(new Order("ls2", new BigDecimal("2"), new BigDecimal("10")));
        absOrderPool.delOrder(new Order("ls3", new BigDecimal("3"), new BigDecimal("10")));
        absOrderPool.delOrder(new Order("ls4", new BigDecimal("2"), new BigDecimal("10")));
    }

    @Test
    void testPrint() {
        absOrderPool.print();
    }

}
