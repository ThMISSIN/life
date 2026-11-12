package com.life.config;

import com.life.interceptor.InternalTokenInterceptor;
import com.life.interceptor.JwtAdminInterceptor;
import com.life.interceptor.JwtUserInterceptor;
import com.life.interceptor.SentinelLimitInterceptor;
import com.life.json.JacksonObjectMapper;
import com.life.properties.AgentProperties;
import com.life.properties.JwtProperties;
import com.life.properties.SentinelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private AgentProperties agentProperties;

    @Autowired
    private SentinelProperties sentinelProperties;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${life.local-storage.base-path:uploads}")
    private String localStorageBasePath;

    /**
     * 注册自定义拦截器：需登录的接口（token校验+登出黑名单校验），
     * 登录、注册、忘记密码为公开接口。
     * /user/** 用户接口, /benefit/** 权益C端接口, /cart/** 购物车接口, /ai/** Agent接口
     * <p>2.3 起 /admin/** 由 JwtAdminInterceptor 独立鉴权(管理端令牌+userType校验),
     * 唯一例外 /admin/common/**(C端前端在用的上传通道, 历史路径, 仍走C端登录态)
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 实例1：必登录拦截器，游客或无效令牌401
        registry.addInterceptor(new JwtUserInterceptor(jwtProperties, stringRedisTemplate, true))
                .addPathPatterns("/user/**", "/benefit/**", "/signRecord/**", "/points/**", "/post/**", "/follow/**", "/like/**", "/product/**", "/replies/**", "/order/**", "/cart/**", "/notice/**", "/favorite/**", "/ai/**", "/admin/common/**")
                .excludePathPatterns(
                        "/user/user/login",
                        "/user/user/register",
                        "/user/user/forget",
                        "/user/user/logout",
                        "/replies/page");
        // 实例2：可选登录拦截器，带有效令牌则解析用户身份，否则按匿名放行
        registry.addInterceptor(new JwtUserInterceptor(jwtProperties, stringRedisTemplate, false))
                .addPathPatterns("/replies/page");
        // 实例3：内部服务拦截器（Python Agent 工具网关专用）：共享令牌校验 + X-User-Id 注入登录态
        registry.addInterceptor(new InternalTokenInterceptor(agentProperties))
                .addPathPatterns("/ai-tools/**");
        // 实例4(2.3)：管理端拦截器——独立密钥+独立令牌头(默认admin-authentication)+userType=ADMIN
        // 三重校验, 无权限403; C端令牌打管理端/管理端令牌打C端互相不可用;
        // 管理端登录/登出与C端上传通道(/admin/common/**, 历史路径)除外
        registry.addInterceptor(new JwtAdminInterceptor(jwtProperties, stringRedisTemplate))
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/user/login", "/admin/user/logout", "/admin/common/**");
        // 实例5(2.10)：Sentinel 限流埋点(资源级总QPS + 单用户热点参数), 须在身份拦截器之后取到userId
        registry.addInterceptor(new SentinelLimitInterceptor(sentinelProperties))
                .addPathPatterns("/**")
                .excludePathPatterns("/uploads/**");
    }

    /**
     * 本地存储静态映射: /uploads/** -> {base-path} 目录
     * （life.storage.type=local 时上传文件的对外访问通道）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(localStorageBasePath).toAbsolutePath().toString().replace("\\", "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolute + "/");
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
