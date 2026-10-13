package com.shequ.module.user.entity;

/**
 * 用户状态。
 */
public enum UserStatus {

    /** 正常 */
    NORMAL,

    /** 被封禁：不可登录、不可发布内容 */
    BANNED,

    /** 注销处理中：7 天内禁止登录与发布，到期由定时任务清理 */
    DEACTIVATING,

    /** 已注销：数据已清理（软删 + 手机号释放），仅作历史留痕 */
    DELETED

}