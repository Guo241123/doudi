package com.shequ.module.message.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * #36 发起/复用会话返回体（接口文档 2.7）。
 * <p>
 * 注意必须是对象 {@code {conversationId}}，不能直接返回裸 long——前端按文档取
 * {@code data.conversationId}，返回裸数字会拿不到值，导致"联系卖家/联系负责人"静默失败。
 */
@Data
@AllArgsConstructor
public class ConversationCreateVO {

    private Long conversationId;
}
