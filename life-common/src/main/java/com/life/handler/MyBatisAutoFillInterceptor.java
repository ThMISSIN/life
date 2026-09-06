package com.life.handler;

import com.life.context.BaseContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Properties;

/**
 * MyBatis 公共字段自动填充拦截器
 * <p>
 * 针对原生 MyBatis（非 MyBatis Plus）场景，
 * 在 INSERT/UPDATE 时自动填充 createTime、updateTime、deleted、creater、updater。
 */
@Component
@Intercepts({
    @Signature(type = Executor.class, method = "update",
               args = {MappedStatement.class, Object.class})
})
public class MyBatisAutoFillInterceptor implements Interceptor {

    private static final String CREATE_TIME = "createTime";
    private static final String UPDATE_TIME = "updateTime";
    private static final String DELETED     = "deleted";
    private static final String CREATER     = "creater";
    private static final String UPDATER     = "updater";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter      = invocation.getArgs()[1];

        if (parameter == null) {
            return invocation.proceed();
        }

        SqlCommandType type = ms.getSqlCommandType();
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = BaseContext.getCurrentId();

        if (type == SqlCommandType.INSERT) {
            setFieldIfExists(parameter, CREATE_TIME, now);
            setFieldIfExists(parameter, UPDATE_TIME, now);
            setFieldIfExists(parameter, DELETED, 0);
            if (currentUserId != null) {
                setFieldIfExists(parameter, CREATER, currentUserId);
                setFieldIfExists(parameter, UPDATER, currentUserId);
            }
        } else if (type == SqlCommandType.UPDATE) {
            setFieldIfExists(parameter, UPDATE_TIME, now);
            if (currentUserId != null) {
                setFieldIfExists(parameter, UPDATER, currentUserId);
            }
        }

        return invocation.proceed();
    }

    /**
     * 反射设置字段值（字段不存在则跳过）
     */
    private void setFieldIfExists(Object parameter, String fieldName, Object value) {
        // 单参数直接取对象
        Object target = parameter;
        // 多参数时取 Map 包装对象（MyBatis 默认多参数时为 ParamMap）
        if (parameter instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) parameter;
            // 优先找 _parameter（单参数包装），否则遍历
            target = map.containsKey("_parameter") ? map.get("_parameter") : map.values().stream()
                    .filter(v -> {
                        try {
                            return v != null && v.getClass().getDeclaredField(fieldName) != null;
                        } catch (NoSuchFieldException e) {
                            return false;
                        }
                    })
                    .findFirst()
                    .orElse(parameter);
        }

        try {
            java.lang.reflect.Field field = findField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                // 仅在字段值为 null 时填充，避免覆盖显式传入的值
                if (field.get(target) == null) {
                    field.set(target, value);
                }
            }
        } catch (Exception ignored) {
            // 字段不存在或设置失败，跳过
        }
    }

    /**
     * 向上递归查找字段（包括父类）
     */
    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // no-op
    }
}
