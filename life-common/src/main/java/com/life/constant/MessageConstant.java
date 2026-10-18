package com.life.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String ALREADY_EXISTS = "已存在";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了菜品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "套餐内包含未启售菜品，无法启售";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";

    // ==================== 权益模块 ====================
    public static final String CATEGORY_ID_REQUIRED = "分类ID不能为空";
    public static final String CATEGORY_NAME_REQUIRED = "分类名称不能为空";
    public static final String CATEGORY_NAME_EXISTS = "分类名称已存在";
    public static final String CATEGORY_PARENT_ERROR = "上级分类不存在或层级不正确";
    public static final String CATEGORY_BE_RELATED = "当前分类下存在子分类或已挂载权益,不能删除";
    public static final String BENEFIT_NAME_REQUIRED = "权益名称不能为空";
    public static final String BENEFIT_BE_USED = "当前权益存在未使用的用户权益,不能删除";
    public static final String BENEFIT_NOT_FOUND = "权益不存在或已下架";

    public static final String USER_LEVEL_NOT_ENOUGH = "当前用户的会员等级不够";
    public static final String BENEFIT_STOCK_NOT_ENOUGH = "权益库存不足";
    public static final String USER_EXCHANGED_COUNT_REACH_LIMIT = "当前用户已领权益数量已超限，无法继续领取";
    public static final String USER_POINTS_NOT_ENOUGH = "当前用户积分不足，无法兑换";
    public static final String USER_MONEY_NOT_ENOUGH = "余额不足, 请先充值";
    public static final String BENEFIT_VALID_CONFIG_ERROR = "权益有效期配置异常,请联系客服";
    public static final String BENEFIT_ALREADY_EXPIRED = "该权益已超过兑换有效期";

    // ==================== 商品模块 ====================
    public static final String PRODUCT_ID_REQUIRED = "商品ID不能为空";
    public static final String PRODUCT_NOT_FOUND = "商品不存在或已下架";
    public static final String PRODUCT_STOCK_NOT_ENOUGH = "商品库存不足";
    public static final String PRODUCT_PURCHASE_LIMIT_REACHED = "商品购买数量已达上限";
    public static final String PRODUCT_NAME_REQUIRED = "商品名称不能为空";
    public static final String PRODUCT_ON_SALE = "上架中的商品不能删除,请先下架";
    public static final String PRODUCT_PRICE_CHANNEL_REQUIRED = "商品至少开通一条购买通道(现金或积分)";
    public static final String PRODUCT_CATEGORY_ID_REQUIRED = "商品分类ID不能为空";
    public static final String CATEGORY_BE_RELATED_PRODUCT = "当前分类下存在子分类或已挂载商品,不能删除";
}
