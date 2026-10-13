package com.shequ.module.message.controller;

import com.shequ.common.auth.AdminAuthInterceptor;
import com.shequ.common.result.CursorResult;
import com.shequ.common.result.PageResult;
import com.shequ.common.result.Result;
import com.shequ.module.message.dto.MessageSendDTO;
import com.shequ.module.message.service.MessageService;
import com.shequ.module.message.vo.AdminConversationVO;
import com.shequ.module.message.vo.MessageSendVO;
import com.shequ.module.message.vo.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台站内信工作台（接口文档 2.10 #55）。
 * <p>
 * 管理员在此查看用户发起的会话（任务咨询优先）并回复，用户端消息中心即可看到——
 * 这是「客服对话 / 联系任务负责人」形成闭环的关键：没有它，用户发出去的消息永远无人回。
 * <p>
 * 管理员 ID 一律从 {@link AdminAuthInterceptor} 写入的登录态取，接口不存在 adminId 入参。
 */
@Tag(name = "管理后台-站内信工作台")
@RestController
@RequestMapping("/api/admin/v1/conversations")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;

    @Operation(summary = "#55 会话列表（TASK_CONSULT 优先）")
    @GetMapping
    public Result<PageResult<AdminConversationVO>> conversations(
            HttpServletRequest request,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(messageService.adminConversations(adminId(request), type, page, size));
    }

    @Operation(summary = "#55 会话内消息（游标翻页，进入即置管理员已读）")
    @GetMapping("/{id}/messages")
    public Result<CursorResult<MessageVO>> messages(
            HttpServletRequest request,
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(messageService.adminMessages(adminId(request), id, cursor, size));
    }

    @Operation(summary = "#55 管理员回复")
    @PostMapping("/{id}/messages")
    public Result<MessageSendVO> reply(HttpServletRequest request,
                                       @PathVariable long id,
                                       @Valid @RequestBody MessageSendDTO dto) {
        return Result.ok(messageService.adminReply(adminId(request), id, dto));
    }

    private long adminId(HttpServletRequest request) {
        Object value = request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_ID);
        return value == null ? 0L : (long) value;
    }

}
