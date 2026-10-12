package com.life.constant;

/**
 * 逻辑删除标记(BaseEntity.deleted, 与MP配置 logic-delete-value=1 / logic-not-delete-value=0 对齐)
 */
public class LogicDeleteConstant {

    /** 未删除 */
    public static final int EXIST = 0;

    /** 已删除 */
    public static final int DELETED = 1;
}
