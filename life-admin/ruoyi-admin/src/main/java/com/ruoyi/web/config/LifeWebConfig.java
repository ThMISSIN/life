package com.ruoyi.web.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * life业务静态资源映射: /uploads/** -> 共享本地存储目录
 * 与C端 life-java WebMvcConfiguration 同口径, C端未启动时管理端也能直接预览图片
 */
@Configuration
public class LifeWebConfig implements WebMvcConfigurer
{
    @Value("${life.local-storage.base-path:uploads}")
    private String localStorageBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        String absolute = Paths.get(localStorageBasePath).toAbsolutePath().toString().replace("\\", "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolute + "/");
    }
}
