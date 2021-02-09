package com.learn.mybatis.service;

import com.learn.mybatis.MainTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = MainTest.class)
class LimitSellServiceTest extends Assertions {

    @Autowired
    LimitSellService limitSellService;

    @BeforeEach
    void before() {
//        limitSellService.
    }

    @Test
    void entry() {
    }

}
