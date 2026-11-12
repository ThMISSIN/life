package com.life.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "life.jwt")
@Data
public class JwtProperties {

    private String userSecretKey;
    private long userTtl;
    private String userTokenName;

    /**
     * 管理端令牌密钥(2.3): 与C端密钥分离——C端令牌无法解析通过管理端拦截器, 反之亦然;
     * 未配置时回退 userSecretKey + userType 声明校验(纵深防御仍有效)
     */
    private String adminSecretKey;
    private long adminTtl = 7200000;
    private String adminTokenName = "admin-authentication";
}
