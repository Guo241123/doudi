package com.shequ.common.constant;

/**
 * Redis 键统一规范：所有键必须在此登记，禁止业务代码里散落字符串拼接。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    /** 图形验证码：2 分钟有效、一次性 */
    public static String captcha(String captchaId) {
        return "captcha:" + captchaId;
    }

    /** 短信验证码：5 分钟有效 */
    public static String smsCode(String scene, String mobile) {
        return "sms:code:" + scene + ":" + mobile;
    }

    /** 短信验证码错误次数：累计 5 次该验证码作废 */
    public static String smsError(String scene, String mobile) {
        return "sms:err:" + scene + ":" + mobile;
    }

    /** 短信限频：同一手机号 1 分钟 1 条 */
    public static String smsMinuteLimit(String mobile) {
        return "sms:limit:min:" + mobile;
    }

    /** 短信限频：同一手机号每日 5 条 */
    public static String smsDailyLimit(String mobile) {
        return "sms:limit:day:" + mobile;
    }

    /** 短信限频：同一 IP 每日上限 */
    public static String smsIpLimit(String ip) {
        return "sms:limit:ip:" + ip;
    }

    /** 注册中间态（手机号 + 密码哈希）：10 分钟有效，完成注册或过期即删 */
    public static String register(String registerToken) {
        return "register:" + registerToken;
    }

    /** 密码登录错误次数 */
    public static String loginError(String mobile) {
        return "login:err:" + mobile;
    }

    /** 密码登录锁定（连续错 5 次锁 15 分钟） */
    public static String loginLock(String mobile) {
        return "login:lock:" + mobile;
    }

    /** 登录限频：同一 IP */
    public static String loginIpLimit(String ip) {
        return "login:ip:" + ip;
    }

    /** 封禁 / 注销黑名单：鉴权统一拦截，即时踢下线 */
    public static String banUser(long userId) {
        return "ban:user:" + userId;
    }

    /** 该用户全部有效 token 的 jti 集合：高危操作整批清除（接口文档实现注记 10） */
    public static String userTokens(long userId) {
        return "user:tokens:" + userId;
    }

    /** 管理员 token 白名单：与用户集合分离，两套 ID 空间互不冲突（接口文档 #47） */
    public static String adminTokens(long adminId) {
        return "admin:tokens:" + adminId;
    }

    /** 管理员登录错误次数（连续错 5 次锁定，与 App 登录同规则） */
    public static String adminLoginError(String username) {
        return "admin:login:err:" + username;
    }

    /** 管理员登录锁定 */
    public static String adminLoginLock(String username) {
        return "admin:login:lock:" + username;
    }

    /** 会话未读数计数器（#40 只查 Redis；进会话清零） */
    public static String convUnread(long conversationId, long userId) {
        return "conv:unread:" + conversationId + ":" + userId;
    }

}