package com.learn.mybatis.core.lua;

import com.learn.mybatis.MainTest;
import com.learn.mybatis.common.Randomizer;
import com.learn.mybatis.domain.Order;
import lombok.Builder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.learn.mybatis.core.lua.OrderPoolLuaHelper.getOrderQueueNamePrefix;

@SpringBootTest(classes = MainTest.class)
class OrderPoolLuaHelperTest extends Assertions {

    static int CPU_CORES = Runtime.getRuntime().availableProcessors();

    final String namespace = "TEST";

    @Autowired
    StringRedisTemplate redisTemplate;

    OrderPoolLuaHelper helper;

    @BeforeEach
    void beforeEach() {
        helper = new OrderPoolLuaHelper(namespace, true, redisTemplate);
    }

    @Test
    void add() {
        List<Order> orders = OrderRandomizer.builder()
                .numberOfPrice(10)
                .numberOfSamePrice(10)
                .priceRandomizer(new Randomizer("0.0001", "100000", 4))
                .amountRandomizer(new Randomizer("0.0001", "100000", 4))
                .build().get();
        // Mirror
        {
            OrderPoolMirror mirror = new OrderPoolMirror(helper.isAscending());
            for (Order order : orders) {
                helper.add(order);
                mirror.add(order);
                assertEquals(mirror.toString(), helper.fetchOrderPool().toString());
            }

            Set<String> keys = helper.getRedisTemplate().keys(getOrderQueueNamePrefix(namespace) + "*");
            assertNotNull(keys);
            assertEquals(mirror.orderPool.size(), keys.size());
        }
    }

    @Test
    void remove() {
        List<Order> orders = OrderRandomizer.builder()
                .numberOfPrice(10)
                .numberOfSamePrice(10)
                .priceRandomizer(new Randomizer("0.0001", "100000", 4))
                .amountRandomizer(new Randomizer("0.0001", "100000", 4))
                .build().get();
        addOrdersToRedis(orders);
        // Mirror
        {
            OrderPoolMirror mirror = new OrderPoolMirror(orders, helper.isAscending());
            for (Order order : orders) {
                helper.remove(order);
                mirror.remove(order);
                assertEquals(mirror.toString(), helper.fetchOrderPool().toString());
            }
        }

        Set<String> keys = helper.getRedisTemplate().keys("*");
        assertNotNull(keys);
        assertEquals(0, keys.size());
    }

    @Test
    void pop() {
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
            OrderPoolMirror mirror = new OrderPoolMirror(orders, helper.isAscending());
            String mirrorBeforeStr = mirror.toString();
            assertEquals(mirrorBeforeStr, helper.fetchOrderPool().toString());

            for (Order order : toBeMatchedOrders) {
                boolean isAscending = ((int) (Math.random() * 10) & 1) == 0;
                assertEquals(mirror.doPop(order.getPrice(), order.getAmount(), isAscending),
                        helper.doPop(order.getPrice(), order.getAmount(), isAscending),
                        String.format("isAscending=%s \nmirrorBefore=%s\n", isAscending, mirrorBeforeStr));
                assertEquals(mirror.orderPool, helper.fetchOrderPool(),
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
            OrderPoolMirror mirror = new OrderPoolMirror(helper.isAscending());
            for (Order order : orders) {
                mirror.add(order);
            }
            ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> orderPool = helper.fetchOrderPool();
            assertEquals(mirror.toString(), orderPool.toString());
        }
    }

    @AfterEach
    void after() {
        helper.getRedisTemplate().execute((RedisCallback<Object>) connection -> connection.execute("FLUSHALL"));
    }

    void addOrdersToRedis(List<Order> orders) {
        orders.forEach(helper::add);
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
        Set<String> keys = helper.getRedisTemplate().keys(getOrderQueueNamePrefix(namespace) + "*");
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
