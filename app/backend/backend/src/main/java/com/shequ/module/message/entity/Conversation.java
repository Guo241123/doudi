package com.shequ.module.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内信会话实体（接口文档 #36～#37）。
 * peer_id：TRADE=卖家用户 ID；TASK_CONSULT / CUSTOMER=管理员 ID。
 */
@Data
@TableName("t_conversation")
public class Conversation {

    /** 任务咨询 */
    public static final String TYPE_TASK_CONSULT = "TASK_CONSULT";

    /** 联系管理员（客服） */
    public static final String TYPE_CUSTOMER = "CUSTOMER";

    /** 交易咨询（联系卖家） */
    public static final String TYPE_TRADE = "TRADE";

    /** 默认客服 / 任务咨询接待的管理员 ID（种子管理员） */
    public static final long DEFAULT_ADMIN_ID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** TASK_CONSULT / CUSTOMER / TRADE */
    private String type;

    /** 发起用户 ID */
    private Long userId;

    /** 对方 ID */
    private Long peerId;

    /** 关联任务 ID（仅 TASK_CONSULT） */
    private Long taskId;

    /** 最后一条消息摘要 */
    private String lastMessage;

    /** 最后一条消息时间 */
    private LocalDateTime lastTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
