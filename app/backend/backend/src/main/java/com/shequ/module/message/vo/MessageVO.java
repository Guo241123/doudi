package com.shequ.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话内消息（接口文档 #38）。
 */
@Data
@AllArgsConstructor
public class MessageVO {

    private Long messageId;

    private Long senderId;

    private String content;

    private LocalDateTime createTime;

    /** 该消息是否已被接收方读取 */
    private boolean read;

}
