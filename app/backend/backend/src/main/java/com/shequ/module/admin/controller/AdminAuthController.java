package com.shequ.module.admin.controller;

import com.shequ.common.result.Result;
import com.shequ.module.admin.dto.AdminLoginDTO;
import com.shequ.module.admin.service.AdminAuthService;
import com.shequ.module.admin.vo.AdminLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台认证接口（接口文档 #47）。
 * <p>
 * 登录路径在 AdminAuthInterceptor 的豁免范围内（WebMvcConfig 配置），
 * 其余 /api/admin/v1/** 全部要求 ADMIN token。
 */
@Tag(name = "管理后台-认证")
@RestController
@RequestMapping("/api/admin/v1/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "#47 管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.ok(adminAuthService.login(dto));
    }

}
