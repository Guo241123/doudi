package com.shequ.module.message.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项（接口文档 #37）。
 */
@Data
public class ConversationVO {

    private Long conversationId;

    /** TASK_CONSULT / CUSTOMER / TRADE */
    private String type;

    /** 对方（TRADE=卖家；TASK_CONSULT / CUSTOMER=管理员） */
    private Peer peer;

    /** 仅任务咨询：咨询的是哪个任务 */
    private TaskRef taskRef;

    private String lastMessage;

    private LocalDateTime lastTime;

    private Integer unreadCount;

    @Data
    public static class Peer {

        private Long userId;

        private String nickname;

        private String avatarUrl;

    }

    @Data
    public static class TaskRef {

        private Long taskId;

        private String title;

    }

}
