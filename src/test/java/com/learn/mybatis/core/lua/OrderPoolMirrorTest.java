package com.learn.mybatis.core.lua;

import com.learn.mybatis.MainTest;
import com.learn.mybatis.domain.Order;
import com.learn.mybatis.domain.OrderPoolPopResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author Jover Zhang
 */
@SpringBootTest(classes = MainTest.class)
public class OrderPoolMirrorTest extends Assertions {

    @Test
    void popByPrice() {
        final int PRECISION = 6;
        List<TestSet> testSets = new ArrayList<TestSet>() {{
            add(new TestSet(
                    new ArrayList<Order>() {{
                        add(new Order("s1", "2", "10"));
                    }},
                    new BigDecimal("0"),
                    OrderPoolPopResult.build(BigDecimal.ZERO, null, Collections.emptyList()),
                    new ConcurrentSkipListMap<BigDecimal, List<Order>>() {{
                        put(new BigDecimal("2"), new ArrayList<Order>() {{
                            add(new Order("s1", "2", "10"));
                        }});
                    }}));
            add(new TestSet(
                    new ArrayList<Order>() {{
                        add(new Order("s1", "2", "10"));
                        add(new Order("s2", "3", "20"));
                    }},
                    new BigDecimal("30"),
                    OrderPoolPopResult.build(BigDecimal.ZERO, null, new ArrayList<Order>() {{
                        add(new Order("s1", "2", "10"));
                        add(new Order("s2", "3", "3.333333"));
                    }}),
                    new ConcurrentSkipListMap<BigDecimal, List<Order>>() {{
                        put(new BigDecimal("3"), new ArrayList<Order>() {{
                            add(new Order("s2", "3", "16.666667"));
                        }});
                    }}));
            add(new TestSet(
                    new ArrayList<Order>() {{
                        add(new Order("s1", "2", "2"));
                        add(new Order("s2", "2", "3"));
                        add(new Order("s3", "2", "5"));
                        add(new Order("s4", "3", "1"));
                        add(new Order("s5", "3", "2"));
                        add(new Order("s6", "3", "3"));
                    }},
                    new BigDecimal("30"),
                    OrderPoolPopResult.build(BigDecimal.ZERO, null, new ArrayList<Order>() {{
                        add(new Order("s1", "2", "2"));
                        add(new Order("s2", "2", "3"));
                        add(new Order("s3", "2", "5"));
                        add(new Order("s4", "3", "1"));
                        add(new Order("s5", "3", "2"));
                        add(new Order("s6", "3", "0.333333"));
                    }}),
                    new ConcurrentSkipListMap<BigDecimal, List<Order>>() {{
                        put(new BigDecimal("3"), new ArrayList<Order>() {{
                            add(new Order("s6", "3", "2.666667"));
                        }});
                    }}));
            add(new TestSet(
                    new ArrayList<Order>() {{
                        add(new Order("s1", "2", "10"));
                        add(new Order("s2", "3", "20"));
                    }},
                    new BigDecimal("80"),
                    OrderPoolPopResult.build(BigDecimal.ZERO, null, new ArrayList<Order>() {{
                        add(new Order("s1", "2", "10"));
                        add(new Order("s2", "3", "20"));
                    }}),
                    new ConcurrentSkipListMap<>()));
            add(new TestSet(
                    new ArrayList<Order>() {{
                        add(new Order("s1", "2", "10"));
                        add(new Order("s2", "3", "20"));
                    }},
                    new BigDecimal("81"),
                    OrderPoolPopResult.build(new BigDecimal("0.999999"), null, new ArrayList<Order>() {{
                        add(new Order("s1", "2", "10"));
                        add(new Order("s2", "3", "20"));
                    }}),
                    new ConcurrentSkipListMap<>()));
        }};

        for (TestSet testSet : testSets) {
            OrderPoolMirror mirror = new OrderPoolMirror(testSet.getOrders(), true);
            OrderPoolPopResult result = mirror.popByPrice(testSet.getSumPrice(), PRECISION);
            assertEquals(testSet.getResult(), result);
            assertEquals(testSet.getAfterOrderPool(), mirror.orderPool);
        }
    }

    @Getter
    @AllArgsConstructor
    static class TestSet {

        // 挂单池中的挂单
        List<Order> orders;

        // For popByPrice
        BigDecimal sumPrice;

        // 方法返回值
        OrderPoolPopResult result;

        // 方法执行后 挂单池 的状态
        ConcurrentSkipListMap<BigDecimal, List<Order>> afterOrderPool;

    }

}
