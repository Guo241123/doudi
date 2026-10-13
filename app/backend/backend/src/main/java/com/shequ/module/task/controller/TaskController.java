package com.shequ.module.task.controller;

import com.shequ.common.result.CursorResult;
import com.shequ.common.result.Result;
import com.shequ.module.task.service.TaskService;
import com.shequ.module.task.vo.TaskDetailVO;
import com.shequ.module.task.vo.TaskItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务接口（接口文档 2.2 #15 / #16，App 只读、游客可访问）。
 * <p>
 * 后台发布 / 编辑 / 下架 / 删除（#49）在 /api/admin/v1，待 admin 模块落地后实现。
 */
@Tag(name = "任务")
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "#15 任务信息流")
    @GetMapping
    public Result<CursorResult<TaskItemVO>> feed(@RequestParam(defaultValue = "0") int cursor,
                                                @RequestParam(defaultValue = "20") int size) {
        return Result.ok(taskService.feed(cursor, size));
    }

    @Operation(summary = "#16 任务详情")
    @GetMapping("/{id}")
    public Result<TaskDetailVO> detail(@PathVariable long id) {
        return Result.ok(taskService.detail(id));
    }

}
