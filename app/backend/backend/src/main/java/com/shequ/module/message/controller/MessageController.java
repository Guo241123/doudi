package com.shequ.module.message.controller;

import com.shequ.common.auth.AuthInterceptor;
import com.shequ.common.result.CursorResult;
import com.shequ.common.result.Result;
import com.shequ.module.message.dto.ConversationCreateDTO;
import com.shequ.module.message.dto.MessageSendDTO;
import com.shequ.module.message.service.MessageService;
import com.shequ.module.message.vo.ConversationCreateVO;
import com.shequ.module.message.vo.ConversationVO;
import com.shequ.module.message.vo.MessageSendVO;
import com.shequ.module.message.vo.MessageVO;
import com.shequ.module.message.vo.UnreadCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内信接口（接口文档 2.7 #36～#40）。
 * 轮询节奏（会话页 5s / 前台 30s / 后台停止）由前端控制，#40 只查 Redis。
 */
@Tag(name = "消息")
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "#36 发起 / 复用会话")
    @PostMapping("/conversations")
    public Result<ConversationCreateVO> create(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                               @Valid @RequestBody ConversationCreateDTO dto) {
        return Result.ok(new ConversationCreateVO(messageService.createConversation(userId, dto)));
    }

    @Operation(summary = "#37 会话列表")
    @GetMapping("/conversations")
    public Result<CursorResult<ConversationVO>> conversations(
            @RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(messageService.conversations(userId, type, cursor, size));
    }

    @Operation(summary = "#38 会话内消息（进入自动已读）")
    @GetMapping("/conversations/{id}/messages")
    public Result<CursorResult<MessageVO>> messages(
            @RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(messageService.messages(userId, id, cursor, size));
    }

    @Operation(summary = "#39 发送消息")
    @PostMapping("/conversations/{id}/messages")
    public Result<MessageSendVO> send(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                      @PathVariable long id,
                                      @Valid @RequestBody MessageSendDTO dto) {
        return Result.ok(messageService.send(userId, id, dto));
    }

    @Operation(summary = "#40 未读数")
    @GetMapping("/unread-count")
    public Result<UnreadCountVO> unreadCount(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId) {
        return Result.ok(messageService.unreadCount(userId));
    }

}
