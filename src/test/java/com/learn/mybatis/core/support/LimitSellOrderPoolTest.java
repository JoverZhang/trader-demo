package com.learn.mybatis.core.support;

import com.learn.mybatis.MainTest;
import com.learn.mybatis.core.lua.OrderPoolMirror;
import com.learn.mybatis.domain.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;

import java.util.*;

@SpringBootTest(classes = MainTest.class)
class LimitSellOrderPoolTest extends Assertions {

    @Autowired
    LimitSellOrderPool orderPool;

    @Test
    void match() {
        ArrayList<Order> orders = new ArrayList<Order>() {{
            for (int i = 1; i <= 10; i++) {
                add(new Order("s" + i, String.valueOf(i), "10"));
            }
        }};
        Map<Order, List<Order>> toBeMatchedOrderMap = new LinkedHashMap<Order, List<Order>>() {{
            // 外单价格不足
            put(new Order("b1", "0.5", "10000"), Collections.emptyList());
            // 完整匹配
            put(new Order("b2", "2", "20"), new LinkedList<Order>() {{
                add(new Order("s1", "1", "10"));
                add(new Order("s2", "2", "10"));
            }});
            // 外单数量购尽
            put(new Order("b3", "10000", "15"), new LinkedList<Order>() {{
                add(new Order("s3", "3", "10"));
                add(new Order("s4", "4", "5"));
            }});
            // 内单数量不足, 外单数量有余
            put(new Order("b4", "5.5", "10000"), new LinkedList<Order>() {{
                add(new Order("s4", "4", "5"));
                add(new Order("s5", "5", "10"));
            }});
            // 高精度测试 1
            put(new Order("b5", "7.000001", "19.999999"), new LinkedList<Order>() {{
                add(new Order("s6", "6", "10"));
                add(new Order("s7", "7", "9.999999"));
            }});
            // 高精度测试 2
            put(new Order("b6", "9.000001", "10.000002"), new LinkedList<Order>() {{
                add(new Order("s7", "7", "0.000001"));
                add(new Order("s8", "8", "10"));
                add(new Order("s9", "9", "0.000001"));
            }});
            // 高精度测试 3 购尽内单
            put(new Order("b5", "10.000001", "10000"), new LinkedList<Order>() {{
                add(new Order("s9", "9", "9.999999"));
                add(new Order("s10", "10", "10"));
            }});
        }};
        // Mirror
        {
            orders.forEach(orderPool::addOrder);
            OrderPoolMirror mirror = new OrderPoolMirror(orders);
            toBeMatchedOrderMap.forEach((order, set) -> {
                List<Order> mirrorMatched = mirror.match(order, orderPool.isAscending());
                List<Order> orderPoolMatched = orderPool.match(order);
                assertEquals(mirrorMatched, orderPoolMatched);
                assertEquals(set, orderPoolMatched);
            });
        }
    }

    @AfterEach
    void after() {
        orderPool.getHelper().getRedisTemplate()
                .execute((RedisCallback<Object>) connection -> connection.execute("FLUSHALL"));
    }

}
