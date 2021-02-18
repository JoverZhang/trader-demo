package com.learn.mybatis.service;

import com.learn.mybatis.MainTest;
import com.learn.mybatis.core.support.LimitBuyOrderPool;
import com.learn.mybatis.core.support.LimitSellOrderPool;
import com.learn.mybatis.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

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
                    Collections.emptyList(),
                    new Order("s1", "100", "0")));
            add(new TestSet(
                    new Order("s1", "100", "100"),
                    new LinkedList<Order>() {{
                        add(new Order("b1", "100", "20"));
                        add(new Order("b2", "100", "20"));
                    }},
                    new Order("s1", "100", "60")));
            add(new TestSet(
                    new Order("s1", "100", "100"),
                    new LinkedList<Order>() {{
                        add(new Order("b1", "100", "50"));
                        add(new Order("b2", "100", "50"));
                    }},
                    new Order("s1", "100", "0")));
        }};

        for (TestSet testSet : testSets) {
            Mockito.when(limitBuyOrderPool.pop(testSet.getParam().getPrice(), testSet.getParam().getAmount()))
                    .thenReturn(testSet.getResult());

            // 该入参会在方法内部被改变
            Order entryParam = testSet.getParam();
            List<Order> matchedOrders = limitSellService.entry(entryParam);

            assertEquals(testSet.getResult(), matchedOrders);
            assertEquals(testSet.getAfterParam(), entryParam);
        }
    }

    @Getter
    @AllArgsConstructor
    static class TestSet {

        Order param;

        List<Order> result;

        Order afterParam;

    }

}
