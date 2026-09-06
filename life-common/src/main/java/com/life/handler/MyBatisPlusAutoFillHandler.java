package com.life.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.life.constant.AutoFillConstant;
import com.life.context.BaseContext;
import com.life.enumeration.OperationType;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 公共字段自动填充处理器
 */
@Component
public class MyBatisPlusAutoFillHandler implements MetaObjectHandler {

    /**
     * 插入操作自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        this.strictInsertFill(metaObject, "creater", Long.class, BaseContext.getCurrentId());
        this.strictInsertFill(metaObject, "updater", Long.class, BaseContext.getCurrentId());
    }

    /**
     * 更新操作自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updater", Long.class, BaseContext.getCurrentId());
    }
}
