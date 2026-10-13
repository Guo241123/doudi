package com.shequ;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 社区任务与二手交易 App 后端启动类（模块化单体）。
 */
@EnableScheduling
@SpringBootApplication
@MapperScan(basePackages = {"com.shequ.module", "com.shequ.common"}, annotationClass = Mapper.class)
public class ShequApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShequApplication.class, args);
    }

}