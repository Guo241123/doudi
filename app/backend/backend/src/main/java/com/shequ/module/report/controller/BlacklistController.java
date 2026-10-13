package com.shequ.module.report.controller;

import com.shequ.common.auth.AuthInterceptor;
import com.shequ.common.result.CursorResult;
import com.shequ.common.result.Result;
import com.shequ.module.report.service.BlacklistService;
import com.shequ.module.report.vo.BlacklistItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 拉黑接口（接口文档 #43/#44）。
 */
@Tag(name = "拉黑")
@RestController
@RequestMapping("/api/v1/blacklist")
@RequiredArgsConstructor
public class BlacklistController {

    private final BlacklistService blacklistService;

    @Operation(summary = "#43 拉黑用户")
    @PostMapping("/{userId}")
    public Result<Void> block(HttpServletRequest request, @PathVariable long userId) {
        blacklistService.block(userId(request), userId);
        return Result.ok();
    }

    @Operation(summary = "#43 解除拉黑")
    @DeleteMapping("/{userId}")
    public Result<Void> unblock(HttpServletRequest request, @PathVariable long userId) {
        blacklistService.unblock(userId(request), userId);
        return Result.ok();
    }

    @Operation(summary = "#44 拉黑列表")
    @GetMapping
    public Result<CursorResult<BlacklistItemVO>> list(HttpServletRequest request,
                                                      @RequestParam(defaultValue = "0") int cursor,
                                                      @RequestParam(defaultValue = "20") int size) {
        return Result.ok(blacklistService.list(userId(request), cursor, size));
    }

    private long userId(HttpServletRequest request) {
        Object value = request.getAttribute(AuthInterceptor.ATTR_USER_ID);
        return value == null ? 0L : (long) value;
    }

}
