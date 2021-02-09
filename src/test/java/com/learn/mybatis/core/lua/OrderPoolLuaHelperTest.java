package com.learn.mybatis.core.lua;

import com.learn.mybatis.MainTest;
import com.learn.mybatis.common.Randomizer;
import com.learn.mybatis.domain.Order;
import lombok.Builder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@SpringBootTest(classes = MainTest.class)
class OrderPoolLuaHelperTest extends Assertions {

    static int CPU_CORES = Runtime.getRuntime().availableProcessors();

    final String namespace = "TEST";

    @Autowired
    OrderPoolLuaHelper helper;

    @Test
    void addOrder() {
        List<Order> orders = OrderRandomizer.builder()
                .numberOfPrice(10)
                .numberOfSamePrice(10)
                .priceRandomizer(new Randomizer("0.0001", "100000", 4))
                .amountRandomizer(new Randomizer("0.0001", "100000", 4))
                .build().get();
        // Mirror
        {
            OrderPoolMirror mirror = new OrderPoolMirror();
            for (Order order : orders) {
                helper.addOrder(namespace, order.getId(),
                        order.getPrice().toPlainString(), order.getAmount().toPlainString());
                mirror.addOrder(order);
                assertEquals(mirror.toString(), helper.fetchOrderPool(namespace).toString());
            }

            Set<String> keys = helper.getRedisTemplate().keys(helper.getOrderQueueNamePrefix(namespace) + "*");
            assertNotNull(keys);
            assertEquals(mirror.orderPool.size(), keys.size());
        }
    }

    @Test
    void delOrder() {
        List<Order> orders = OrderRandomizer.builder()
                .numberOfPrice(10)
                .numberOfSamePrice(10)
                .priceRandomizer(new Randomizer("0.0001", "100000", 4))
                .amountRandomizer(new Randomizer("0.0001", "100000", 4))
                .build().get();
        addOrdersToRedis(orders);
        // Mirror
        {
            OrderPoolMirror mirror = new OrderPoolMirror(orders);
            for (Order order : orders) {
                helper.delOrder(namespace, order.getId(), order.getPrice().toPlainString());
                mirror.delOrder(order);
                assertEquals(mirror.toString(), helper.fetchOrderPool(namespace).toString());
            }
        }

        Set<String> keys = helper.getRedisTemplate().keys("*");
        assertNotNull(keys);
        assertEquals(0, keys.size());
    }

    @Test
    void match() {
        List<Order> orders = OrderRandomizer.builder()
                .numberOfPrice(10)
                .numberOfSamePrice(10)
                .priceRandomizer(new Randomizer("0.0001", "100000", 4))
                .amountRandomizer(new Randomizer("0.0001", "100000", 4))
                .build().get();
        addOrdersToRedis(orders);
        List<Order> toBeMatchedOrders = OrderRandomizer.builder()
                .numberOfPrice(10)
                .numberOfSamePrice(10)
                .priceRandomizer(new Randomizer("0.0001", "100000", 4))
                .amountRandomizer(new Randomizer("0.0001", "100000", 4))
                .build().get();
        // Mirror
        {
            OrderPoolMirror mirror = new OrderPoolMirror(orders);
            String mirrorBeforeStr = mirror.toString();
            assertEquals(mirrorBeforeStr, helper.fetchOrderPool(namespace).toString());

            for (Order order : toBeMatchedOrders) {
                boolean isAscending = ((int) (Math.random() * 10) & 1) == 0;
                assertEquals(mirror.match(order, isAscending),
                        helper.match(namespace, order.getPrice().toPlainString(),
                                order.getAmount().toPlainString(), isAscending),
                        String.format("isAscending=%s \nmirrorBefore=%s\n", isAscending, mirrorBeforeStr));
                assertEquals(mirror.orderPool, helper.fetchOrderPool(namespace),
                        String.format("isAscending=%s \nmirrorBefore=%s\n", isAscending, mirrorBeforeStr));
            }
        }
    }

    @Test
    void fetchOrderPool() {
        List<Order> orders = OrderRandomizer.builder()
                .numberOfPrice(10)
                .numberOfSamePrice(10)
                .priceRandomizer(new Randomizer("0.0001", "100000", 4))
                .amountRandomizer(new Randomizer("0.0001", "100000", 4))
                .build().get();
        addOrdersToRedis(orders);
        // Mirror
        {
            OrderPoolMirror mirror = new OrderPoolMirror();
            for (Order order : orders) {
                mirror.addOrder(order);
            }
            ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> orderPool = helper.fetchOrderPool(namespace);
            assertEquals(mirror.toString(), orderPool.toString());
        }
    }

    @AfterEach
    void after() {
        helper.getRedisTemplate().execute((RedisCallback<Object>) connection -> connection.execute("FLUSHALL"));
    }

    void addOrdersToRedis(List<Order> orders) {
        for (Order order : orders) {
            helper.addOrder(namespace, order.getId(), order.getPrice().toPlainString(), order.getAmount().toPlainString());
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    void parallelAddOrdersToRedis(List<Order> orders) {
        List<Order>[] slots = new List[CPU_CORES];
        for (int i = 0; i < orders.size(); i++) {
            if (slots[i % CPU_CORES] == null) {
                slots[i % CPU_CORES] = new LinkedList<>();
            }
            slots[i % CPU_CORES].add(orders.get(i));
        }

        CountDownLatch latch = new CountDownLatch(CPU_CORES);
        for (List<Order> slot : slots) {
            new Thread(() -> {
                addOrdersToRedis(slot);
                latch.countDown();
            }).start();
        }
        latch.await();

        Set<String> expectPrices = orders.stream().map(o -> o.getPrice().toPlainString()).collect(Collectors.toSet());
        Set<String> keys = helper.getRedisTemplate().keys(helper.getOrderQueueNamePrefix(namespace) + "*");
        assertNotNull(keys);
        assertEquals(expectPrices.size(), keys.size());
    }

    @Builder
    static class OrderRandomizer {

        int numberOfPrice;

        /**
         * Default of {@link #numberOfSamePriceRandomizer}.getInt()
         */
        int numberOfSamePrice;

        Randomizer numberOfSamePriceRandomizer;

        Randomizer priceRandomizer;

        Randomizer amountRandomizer;

        public List<Order> get() {
            assertTrue(numberOfPrice > 0);
            return new LinkedList<Order>() {{
                for (int i = 0; i < numberOfPrice; i++) {
                    BigDecimal price = priceRandomizer.getBigDecimal();
                    int count = numberOfSamePriceRandomizer != null ?
                            numberOfSamePriceRandomizer.getInt() :
                            numberOfSamePrice;
                    for (int j = 0; j < count; j++) {
                        add(Order.builder()
                                .id(i + ":" + j)
                                .price(price)
                                .amount(amountRandomizer.getBigDecimal())
                                .build());
                    }
                }
            }};
        }

    }

}
