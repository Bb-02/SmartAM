package com.chengmaomao.smartam;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.chengmaomao.smartam.tenant.mapper")
public class SmartAmApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartAmApplication.class, args);
    }

}
