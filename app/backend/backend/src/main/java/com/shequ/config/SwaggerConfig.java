package com.shequ.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger（SpringDoc）配置：按 App 端 / 管理后台两组拆分，与接口文档两大节对应。
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("社区任务与二手交易 App 接口")
                .version("v1.1")
                .description("依据《接口文档》V1.1 实现；具体字段以本页面自动生成为准"));
    }

    @Bean
    public GroupedOpenApi appApi() {
        return GroupedOpenApi.builder()
                .group("app")
                .displayName("App 端")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("管理后台")
                .pathsToMatch("/api/admin/v1/**")
                .build();
    }

}