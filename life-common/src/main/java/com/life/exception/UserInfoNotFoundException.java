package com.life.exception;

/**
 * 用户信息不存在异常
 */
public class UserInfoNotFoundException extends BaseException {

    public UserInfoNotFoundException() {
    }

    public UserInfoNotFoundException(String msg) {
        super(msg);
    }
}
