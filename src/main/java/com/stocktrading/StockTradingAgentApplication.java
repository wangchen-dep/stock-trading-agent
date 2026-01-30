package com.stocktrading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class StockTradingAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockTradingAgentApplication.class, args);
    }
}
