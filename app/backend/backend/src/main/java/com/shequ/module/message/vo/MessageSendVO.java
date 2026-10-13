package com.shequ.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发送消息返回体（接口文档 #39）。
 */
@Data
@AllArgsConstructor
public class MessageSendVO {

    private Long messageId;

    private LocalDateTime createTime;

}
