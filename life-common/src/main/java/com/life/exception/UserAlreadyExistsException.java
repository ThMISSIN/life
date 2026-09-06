package com.life.exception;

/**
 * 用户已存在异常（注册时用户名/手机号/邮箱重复）
 */
public class UserAlreadyExistsException extends BaseException {

    public UserAlreadyExistsException() {
    }

    public UserAlreadyExistsException(String msg) {
        super(msg);
    }
}
