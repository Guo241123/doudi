package com.shequ.module.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发起 / 复用会话请求体（接口文档 #36）。
 */
@Data
public class ConversationCreateDTO {

    @NotBlank(message = "会话类型不能为空")
    @Pattern(regexp = "TASK_CONSULT|CUSTOMER|TRADE", message = "会话类型不合法")
    private String type;

    /** 对方用户 ID，TRADE 时必填 */
    private Long peerUserId;

    /** 任务 ID，TASK_CONSULT 时必填 */
    private Long taskId;

}
