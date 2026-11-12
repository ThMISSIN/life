package com.life.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 4.10 接口文档(SpringDoc): /swagger-ui.html 可视化调试, /v3/api-docs 原始文档;
 * C端令牌请求头 authentication / 管理端 admin-authentication 在 Swagger "Authorize" 中分别填写
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lifeSocialOpenApi() {
        return new OpenAPI().info(new Info()
                .title("life-social 全能生活权益汇 API")
                .version("2026-11")
                .description("C端社交/积分/权益/订单接口 + /admin/** 管理端接口(独立管理端令牌)。"
                        + "鉴权: C端请求头 authentication, 管理端请求头 admin-authentication"));
    }
}
