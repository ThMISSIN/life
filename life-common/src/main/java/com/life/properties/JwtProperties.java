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

}
