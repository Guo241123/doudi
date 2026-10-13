package com.shequ.common.constant;

/**
 * 校验正则与长度约束集中定义，避免各 DTO 各写一份（与需求文档 4.1.1 密码规则一致）。
 */
public final class ValidationConst {

    private ValidationConst() {
    }

    /** 手机号：11 位数字，1 开头 */
    public static final String MOBILE = "^1\\d{10}$";

    /** 密码：8～20 位，必须同时含字母与数字 */
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$";

    /** 短信场景 */
    public static final String SMS_SCENE = "REGISTER|LOGIN|RESET|CHANGE_MOBILE";

    /** 性别 */
    public static final String GENDER = "M|F|U";

    /** 短信验证码：6 位数字 */
    public static final String SMS_CODE = "^\\d{6}$";

}