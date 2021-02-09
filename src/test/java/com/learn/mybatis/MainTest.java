package com.learn.mybatis;

import org.junit.jupiter.api.Assertions;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MainTest extends Assertions {

}
