package com.shequ.module.task.controller;

import com.shequ.common.auth.AdminAuthInterceptor;
import com.shequ.common.result.Result;
import com.shequ.module.task.dto.AdminTaskSaveDTO;
import com.shequ.module.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台任务管理接口（接口文档 #49，/api/admin/v1 全部需管理员 JWT）。
 * <p>
 * 管理员 ID 一律从 AdminAuthInterceptor 写入的登录态取，接口不存在 adminId 入参。
 */
@Tag(name = "管理后台-任务管理")
@RestController
@RequestMapping("/api/admin/v1/tasks")
@RequiredArgsConstructor
public class AdminTaskController {

    private final TaskService taskService;

    @Operation(summary = "#49 发布任务")
    @PostMapping
    public Result<Long> publish(HttpServletRequest request, @Valid @RequestBody AdminTaskSaveDTO dto) {
        return Result.ok(taskService.publish(adminId(request), dto));
    }

    @Operation(summary = "#49 编辑任务")
    @PutMapping("/{id}")
    public Result<Void> update(HttpServletRequest request, @PathVariable long id,
                               @Valid @RequestBody AdminTaskSaveDTO dto) {
        taskService.update(adminId(request), id, dto);
        return Result.ok();
    }

    @Operation(summary = "#49 下架任务")
    @PutMapping("/{id}/off-shelf")
    public Result<Void> offShelf(HttpServletRequest request, @PathVariable long id) {
        taskService.offShelf(adminId(request), id);
        return Result.ok();
    }

    @Operation(summary = "#49 删除任务（软删）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @PathVariable long id) {
        taskService.delete(adminId(request), id);
        return Result.ok();
    }

    private long adminId(HttpServletRequest request) {
        Object value = request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN_ID);
        return value == null ? 0L : (long) value;
    }

}
