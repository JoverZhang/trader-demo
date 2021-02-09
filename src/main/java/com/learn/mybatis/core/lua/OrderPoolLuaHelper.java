package com.learn.mybatis.core.lua;

import com.learn.mybatis.domain.Order;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

/**
 * @author Jover Zhang
 */
@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class OrderPoolLuaHelper {

    final private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<Long> addScript;

    private DefaultRedisScript<Long> delScript;

    @SuppressWarnings("rawtypes")
    private DefaultRedisScript<List> matchScript;

    @PostConstruct
    @SuppressWarnings("rawtypes")
    public void init() {
        addScript = new DefaultRedisScript<Long>() {{
            setResultType(Long.class);
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/add_order.lua")));
        }};
        delScript = new DefaultRedisScript<Long>() {{
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/del_order.lua")));
            setResultType(Long.class);
        }};
        matchScript = new DefaultRedisScript<List>() {{
            setResultType(List.class);
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/match_orders.lua")));
        }};
    }

    public void addOrder(@Nonnull String namespace, @Nonnull String id,
                         @Nonnull String price, @Nonnull String amount) {
        redisTemplate.execute(addScript,
                Arrays.asList("namespace", "id", "price", "amount"),
                namespace, id, price, amount);
    }

    public void delOrder(@Nonnull String namespace, @Nonnull String id, @Nonnull String price) {
        redisTemplate.execute(delScript,
                Arrays.asList("namespace", "id", "price"),
                namespace, id, price);
    }

    @SuppressWarnings("unchecked")
    public List<Order> match(@Nonnull String namespace, @Nonnull String price,
                             @Nonnull String amount, boolean isAscending) {
        List<String> matchedOrders = (List<String>) getRedisTemplate()
                .execute(matchScript, Arrays.asList("namespace", "price", "amount", "isAscending"),
                        namespace, price, amount, String.valueOf(isAscending));
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
    public ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> fetchOrderPool(@Nonnull String namespace) {
        ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> result = new ConcurrentSkipListMap<>();

        DefaultRedisScript<List> printScript = new DefaultRedisScript<List>() {{
            setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/fetch_all_orders.lua")));
            setResultType(List.class);
        }};
        List<List<String>> orderPool = redisTemplate
                .execute(printScript, Collections.singletonList("namespace"), namespace);
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
    public void print(@Nonnull String namespace) {
        ConcurrentSkipListMap<BigDecimal, LinkedList<Order>> orderPool = fetchOrderPool(namespace);
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

    /**
     * Just for debug
     */
    @Deprecated
    String getPriceQueueName(String namespace) {
        return namespace + "::prices";
    }

    /**
     * Just for debug
     */
    @Deprecated
    String getOrderQueueNamePrefix(String namespace) {
        return namespace + "::orders::";
    }

    /**
     * Just for debug
     */
    @Deprecated
    String getOrderQueueName(String namespace, String price) {
        return getOrderQueueNamePrefix(namespace) + price;
    }

    /**
     * Just for debug
     */
    @Deprecated
    String getOrderMapName(String namespace) {
        return namespace + "::map";
    }

}
