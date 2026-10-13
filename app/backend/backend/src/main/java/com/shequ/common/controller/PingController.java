package com.shequ.common.controller;

import com.shequ.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 临时连通性检查接口（W1 骨架验收用），验证统一返回体。
 */
@Tag(name = "健康检查")
@RestController
@RequestMapping("/api/v1")
public class PingController {

    @Operation(summary = "连通性检查")
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        return Result.ok(Map.of(
                "status", "up",
                "time", LocalDateTime.now().toString()
        ));
    }

}