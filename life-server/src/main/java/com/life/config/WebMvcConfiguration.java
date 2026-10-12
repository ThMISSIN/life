package com.life.config;

import com.life.interceptor.JwtTokenUserInterceptor;
import com.life.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册自定义拦截器：需登录的接口（token校验+登出黑名单校验），
     * 登录、注册、忘记密码为公开接口。
     * /user/** 用户接口, /benefit/** 权益C端接口, /admin/** 管理端接口
     * (管理端目前仅要求登录, 管理员身份校验待管理端鉴权方案落地后补充)
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**", "/benefit/**", "/admin/**", "/signRecord/**", "/points/**", "/post/**", "/follow/**", "/like/**")
                .excludePathPatterns(
                        "/user/user/login",
                        "/user/user/register",
                        "/user/user/forget",
                        "/user/user/logout");
    }

    /**
     * 扩展Spring MVC框架的消息转换器
     * @param converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 先加默认的
        converters.add(new MappingJackson2HttpMessageConverter());
        // 再加自定义的，排在前面优先使用
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        //设置对象转换器，底层使用Jackson将Java对象转为json
        converter.setObjectMapper(new JacksonObjectMapper());
        //将消息转换器对象追加到mvc框架的转换器集合中
        //0 表示索引 优先使用
        converters.add(0, converter);
    }
}
