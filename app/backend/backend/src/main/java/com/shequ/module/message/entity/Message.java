package com.shequ.module.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内信消息实体（接口文档 #38～#39）。
 */
@Data
@TableName("t_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    /** 发送者 ID（用户或管理员） */
    private Long senderId;

    /** 内容 1～500 字 */
    private String content;

    /** 0 未读 / 1 已读（落库兜底，未读数实时口径走 Redis 计数器） */
    private Integer readFlag;

    private LocalDateTime createTime;

}
