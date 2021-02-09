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
class LimitBuyOrderPoolTest extends Assertions {

    @Autowired
    LimitBuyOrderPool orderPool;

    @Test
    void match() {
        ArrayList<Order> orders = new ArrayList<Order>() {{
            for (int i = 1; i <= 10; i++) {
                add(new Order("b" + i, String.valueOf(i), "10"));
            }
        }};
        LinkedHashMap<Order, List<Order>> toBeMatchedOrderMap = new LinkedHashMap<Order, List<Order>>() {{
            // 外单价格过高
            put(new Order("s1", "10.5", "10000"), Collections.emptyList());
            // 完整匹配
            put(new Order("s2", "8", "20"), new LinkedList<Order>() {{
                add(new Order("b10", "10", "10"));
                add(new Order("b9", "9", "10"));
            }});
            // 外单数量购尽
            put(new Order("s3", "0.000001", "15"), new LinkedList<Order>() {{
                add(new Order("b8", "8", "10"));
                add(new Order("b7", "7", "5"));
            }});
            // 内单数量不足, 外单数量有余
            put(new Order("s4", "5.5", "10000"), new LinkedList<Order>() {{
                add(new Order("b7", "7", "5"));
                add(new Order("b6", "6", "10"));
            }});
            // 高精度测试 1
            put(new Order("s5", "3.999999", "19.999999"), new LinkedList<Order>() {{
                add(new Order("b5", "5", "10"));
                add(new Order("b4", "4", "9.999999"));
            }});
            // 高精度测试 2
            put(new Order("s6", "1.999999", "10.000002"), new LinkedList<Order>() {{
                add(new Order("b4", "4", "0.000001"));
                add(new Order("b3", "3", "10"));
                add(new Order("b2", "2", "0.000001"));
            }});
            // 高精度测试 3 购尽内单
            put(new Order("s7", "0.999999", "10000"), new LinkedList<Order>() {{
                add(new Order("b2", "2", "9.999999"));
                add(new Order("b1", "1", "10"));
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
