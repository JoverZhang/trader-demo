package com.learn.mybatis.service;

import com.learn.mybatis.MainTest;
import com.learn.mybatis.domain.Order;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

@SpringBootTest(classes = MainTest.class)
class LimitBuyServiceTest extends Assertions {

    @Autowired
    LimitBuyService limitBuyService;

    @Test
    void entry() {
        new ArrayList<Order>() {{
            for (int i = 1; i <= 10; i++) {
                add(new Order("ls" + i, String.valueOf(i), "10"));
            }
        }}.forEach(limitBuyService.limitSellOrderPool::addOrder);

        limitBuyService.entry(new Order("lb", "20", "35"));
    }

}
