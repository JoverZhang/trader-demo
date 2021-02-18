package com.learn.mybatis.core.lua;

import com.learn.mybatis.core.support.OrderPool;
import com.learn.mybatis.domain.Order;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * @author Jover Zhang
 */
@Slf4j
@Getter
@SuppressWarnings("rawtypes")
@RequiredArgsConstructor
public class OrderPoolLuaHelper implements OrderPool {

    private static final DefaultRedisScript<Long> ADD_SCRIPT;

    private static final DefaultRedisScript<Long> DEL_SCRIPT;

    private static final DefaultRedisScript<List> MATCH_SCRIPT;

    static {
        ADD_SCRIPT = new DefaultRedisScript<Long>() {{
            setResultType(Long.class);
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/add_order.lua")));
        }};
        DEL_SCRIPT = new DefaultRedisScript<Long>() {{
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/del_order.lua")));
            setResultType(Long.class);
        }};
        MATCH_SCRIPT = new DefaultRedisScript<List>() {{
            setResultType(List.class);
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/match_orders.lua")));
        }};
    }

    final private String namespace;

    final private boolean isAscending;

    final private StringRedisTemplate redisTemplate;

    /**
     * Just for debug
     */
    @Deprecated
    static String getPriceQueueName(String namespace) {
        return namespace + "::prices";
    }

    /**
     * Just for debug
     */
    @Deprecated
    static String getOrderQueueNamePrefix(String namespace) {
        return namespace + "::orders::";
    }

    /**
     * Just for debug
     */
    @Deprecated
    static String getOrderQueueName(String namespace, String price) {
        return getOrderQueueNamePrefix(namespace) + price;
    }

    /**
     * Just for debug
     */
    @Deprecated
    static String getOrderMapName(String namespace) {
        return namespace + "::map";
    }

    @Override
    public void add(@Nonnull Order order) {
        redisTemplate.execute(ADD_SCRIPT,
                Arrays.asList("namespace", "id", "price", "amount"),
                getNamespace(), order.getId(), order.getPrice().toPlainString(), order.getAmount().toPlainString());
    }

    @Override
    public void remove(@Nonnull Order order) {
        redisTemplate.execute(DEL_SCRIPT,
                Arrays.asList("namespace", "id", "price"),
                getNamespace(), order.getId(), order.getPrice().toPlainString());
    }

    @Override
    public List<Order> pop(@Nonnull BigDecimal price, @Nonnull BigDecimal amount) {
        return doPop(price, amount, isAscending());
    }

    @SuppressWarnings("unchecked")
    List<Order> doPop(@Nonnull BigDecimal price, @Nonnull BigDecimal amount, boolean isAscending) {
        List<String> matchedOrders = (List<String>) getRedisTemplate()
                .execute(MATCH_SCRIPT, Arrays.asList("namespace", "price", "amount", "isAscending"),
                        getNamespace(), price.toPlainString(), amount.toPlainString(), String.valueOf(isAscending));
        if (matchedOrders == null || matchedOrders.isEmpty()) {
            return Collections.emptyList();
        }
        return matchedOrders.stream().map(Order::decode).collect(Collectors.toList());
    }

    /**
     * Just for debug
     */
    @Deprecated
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> fetchOrderPool() {
        ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> result = new ConcurrentSkipListMap<>();
        List<List<String>> orderPool = redisTemplate
                .execute(new DefaultRedisScript<List>() {{
                    setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/fetch_all_orders.lua")));
                    setResultType(List.class);
                }}, Collections.singletonList("namespace"), getNamespace());
        if (orderPool == null || orderPool.isEmpty()) {
            return result;
        }

        for (List<String> samePriceOrders : orderPool) {
            for (String orderStr : samePriceOrders) {
                Order order = Order.decode(orderStr);
                result.compute(order.getPrice(), (k, v) -> {
                    if (v == null) {
                        v = new LinkedList<>();
                    }
                    v.add(order);
                    return v;
                });
            }
        }
        return result;
    }

    /**
     * Just for debug
     */
    @Deprecated
    public void print() {
        ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> orderPool = fetchOrderPool();
        if (orderPool == null || orderPool.isEmpty()) {
            System.out.println(orderPool);
            return;
        }

        StringBuilder strBuilder = new StringBuilder();
        for (Map.Entry<BigDecimal, LinkedList<Order>> entry : orderPool.entrySet()) {
            strBuilder.append(String.format("price=%s\n", entry.getKey()));
            for (Order order : entry.getValue()) {
                strBuilder.append(String.format("\t\t id=%s, amount=%s\n", order.getId(), order.getAmount()));
            }
        }

        System.out.println(strBuilder);
    }

}
