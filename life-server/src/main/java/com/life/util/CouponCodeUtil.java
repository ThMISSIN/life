package com.life.util;

import java.security.SecureRandom;

/**
 * 券码生成工具: 短码、可读、便于商家/用户手输或扫码核销。
 * <p>
 * 字符集剔除易混淆的 0/O、1/I/L，分组连字符提升可读性(如 7K4M-Q9X2)；
 * 落库列 life_user_benefit.code 有唯一索引 uk_code，调用方需捕获唯一键冲突重试。
 */
public final class CouponCodeUtil {

    /** 32 进制去混淆字符集(排除 0O1IL) */
    private static final char[] ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 随机体长度(不含连字符) */
    private static final int BODY_LENGTH = 8;
    private static final int GROUP_SIZE = 4;

    private CouponCodeUtil() {
    }

    /**
     * 生成形如 XXXX-XXXX 的短券码(8 位有效字符 + 1 连字符 = 9 字符)
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(BODY_LENGTH + 1);
        for (int i = 0; i < BODY_LENGTH; i++) {
            if (i > 0 && i % GROUP_SIZE == 0) {
                sb.append('-');
            }
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
