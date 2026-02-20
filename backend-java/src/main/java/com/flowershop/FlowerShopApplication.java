package com.flowershop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FLOWER SHOP APPLICATION ENTRY POINT
 * ------------------------------------
 * This is where Spring Boot starts.
 *
 * @SpringBootApplication does three things:
 *                        1. @Configuration → marks this class as a config
 *                        source
 *                        2. @EnableAutoConfiguration → Spring wires up
 *                        everything automatically
 *                        3. @ComponentScan → scans all sub-packages
 *                        (controller, service, repository, model)
 */
@SpringBootApplication
public class FlowerShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowerShopApplication.class, args);
    }
}
