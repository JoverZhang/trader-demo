package com.learn.mybatis.service;

import com.learn.mybatis.MainTest;
import com.learn.mybatis.core.support.LimitBuyOrderPool;
import com.learn.mybatis.core.support.LimitSellOrderPool;
import com.learn.mybatis.domain.Order;
import com.learn.mybatis.domain.OrderPoolPopResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@SpringBootTest(classes = MainTest.class)
class LimitSellServiceTest extends Assertions {

    @Autowired
    LimitSellService limitSellService;

    @MockBean // no override
    LimitSellOrderPool limitSellOrderPool;

    @MockBean
    LimitBuyOrderPool limitBuyOrderPool;

    @Test
    void entry() {
        List<TestSet> testSets = new LinkedList<TestSet>() {{
            add(new TestSet(
                    new Order("s1", "100", "0"),
                    OrderPoolPopResult.build(
                            new BigDecimal("100"), BigDecimal.ZERO,
                            Collections.emptyList())));
            add(new TestSet(
                    new Order("s1", "100", "100"),
                    OrderPoolPopResult.build(
                            new BigDecimal("100"), new BigDecimal("60"),
                            new LinkedList<Order>() {{
                                add(new Order("b1", "100", "20"));
                                add(new Order("b2", "100", "20"));
                            }}
                    )));
            add(new TestSet(
                    new Order("s1", "100", "100"),
                    OrderPoolPopResult.build(
                            new BigDecimal("100"), BigDecimal.ZERO,
                            new LinkedList<Order>() {{
                                add(new Order("b1", "100", "50"));
                                add(new Order("b2", "100", "50"));
                            }}
                    )));
        }};

        for (TestSet testSet : testSets) {
            Mockito.when(limitBuyOrderPool.pop(testSet.getParam().getPrice(), testSet.getParam().getAmount()))
                    .thenReturn(testSet.getResult());

            // 该入参会在方法内部被改变
            Order entryParam = testSet.getParam();
            List<Order> matchedOrders = limitSellService.entry(entryParam);

            assertEquals(testSet.getResult().getOrders(), matchedOrders);
            assertEquals(testSet.getResult().getRemainingPrice(), entryParam.getPrice());
            assertEquals(testSet.getResult().getRemainingAmount(), entryParam.getAmount());
        }
    }

    @Getter
    @AllArgsConstructor
    static class TestSet {

        Order param;

        OrderPoolPopResult result;

    }

}
