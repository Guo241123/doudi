package com.shequ.common.util;

/**
 * 脱敏工具（手机号在展示与日志中一律脱敏，见避坑清单第四节）。
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /** 手机号脱敏：13812345678 → 138****5678 */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

}