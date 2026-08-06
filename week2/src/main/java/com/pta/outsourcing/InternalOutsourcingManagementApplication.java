package com.pta.outsourcing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.pta.outsourcing.mapper")
@SpringBootApplication
public class InternalOutsourcingManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternalOutsourcingManagementApplication.class, args);
    }
}
