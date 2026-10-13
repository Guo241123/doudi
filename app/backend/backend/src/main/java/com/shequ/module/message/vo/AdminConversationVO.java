package com.shequ.module.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台站内信工作台 · 会话列表项（接口文档 #55）。
 * <p>
 * 与 App 端 {@link ConversationVO} 的区别：App 看的是"对方"（peer，交易=卖家、客服=管理员），
 * 后台看的是"发起会话的 App 用户"（user），且未读数是**管理员视角**（用户发来但admin未读的条数）。
 */
@Data
public class AdminConversationVO {

    private Long conversationId;

    /** TASK_CONSULT / CUSTOMER / TRADE */
    private String type;

    /** 发起会话的 App 用户 */
    private UserInfo user;

    /** 仅任务咨询：咨询的是哪个任务 */
    private ConversationVO.TaskRef taskRef;

    private String lastMessage;

    private LocalDateTime lastTime;

    /** 管理员未读（用户发来、管理员还没看的条数；TRADE 会话不在工作台统计） */
    private Integer unreadCount;

    @Data
    public static class UserInfo {

        private Long userId;

        private String nickname;

        private String avatarUrl;

    }

}
