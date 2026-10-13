package com.shequ.module.message.service;

import com.shequ.common.result.CursorResult;
import com.shequ.common.result.PageResult;
import com.shequ.module.message.dto.ConversationCreateDTO;
import com.shequ.module.message.dto.MessageSendDTO;
import com.shequ.module.message.vo.AdminConversationVO;
import com.shequ.module.message.vo.ConversationVO;
import com.shequ.module.message.vo.MessageSendVO;
import com.shequ.module.message.vo.MessageVO;
import com.shequ.module.message.vo.UnreadCountVO;

/**
 * 站内信服务（接口文档 2.7 #36～#40，三类会话同一套；2.10 #55 后台工作台）。
 */
public interface MessageService {

    /** #36 发起 / 复用会话：同类型同对象已存在则复用 */
    long createConversation(long userId, ConversationCreateDTO dto);

    /** #37 会话列表（type 可选筛选，游标分页，带未读数） */
    CursorResult<ConversationVO> conversations(long userId, String type, int cursor, int size);

    /** #38 会话内消息（须为成员；进入会话自动把对方发的消息置已读） */
    CursorResult<MessageVO> messages(long userId, long conversationId, int cursor, int size);

    /** #39 发送消息（1～500 字；存在拉黑关系返回 40301） */
    MessageSendVO send(long userId, long conversationId, MessageSendDTO dto);

    /** #40 未读数（只查 Redis 计数器） */
    UnreadCountVO unreadCount(long userId);

    // ------------------------------------------------------------------ 后台工作台 #55

    /** #55 后台会话列表（页码分页，type 可选，TASK_CONSULT 优先排序，带管理员未读数） */
    PageResult<AdminConversationVO> adminConversations(long adminId, String type, int page, int size);

    /** #55 后台查看会话消息（游标翻页；进入即把用户发来的消息置为管理员已读） */
    CursorResult<MessageVO> adminMessages(long adminId, long conversationId, int cursor, int size);

    /** #55 管理员回复：以管理员身份写入消息，并给 App 用户加未读 */
    MessageSendVO adminReply(long adminId, long conversationId, MessageSendDTO dto);

}
