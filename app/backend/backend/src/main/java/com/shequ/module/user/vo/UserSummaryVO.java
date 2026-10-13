package com.shequ.module.user.vo;

import lombok.Data;

/**
 * 我的信息概览（接口文档 #14，"我的"页面头部）。
 */
@Data
public class UserSummaryVO {

    private UserVO user;

    /** 未读消息数（消息模块落地前恒为 0） */
    private int unreadCount;

    /** 我的发布 - 二手数 */
    private int myItemCount;

    /** 我的发布 - 帖子数 */
    private int myPostCount;

}
