package com.shequ.module.user.controller;

import com.shequ.common.auth.AuthInterceptor;
import com.shequ.common.result.Result;
import com.shequ.module.user.dto.DeactivateDTO;
import com.shequ.module.user.dto.MobileChangeDTO;
import com.shequ.module.user.dto.PasswordChangeDTO;
import com.shequ.module.user.dto.ProfileUpdateDTO;
import com.shequ.module.user.service.AuthService;
import com.shequ.module.user.service.UserService;
import com.shequ.module.user.vo.UserProfileVO;
import com.shequ.module.user.vo.UserSummaryVO;
import com.shequ.module.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号与个人资料接口（接口文档 2.1 #9～#14）。
 */
@Tag(name = "认证与账号")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "#9 注销账号")
    @PostMapping("/deactivate")
    public Result<Void> deactivate(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                   @Valid @RequestBody DeactivateDTO dto) {
        authService.deactivate(userId, dto);
        return Result.ok();
    }

    @Operation(summary = "#10 我的资料")
    @GetMapping("/profile")
    public Result<UserProfileVO> profile(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId) {
        return Result.ok(userService.getProfile(userId));
    }

    @Operation(summary = "#11 编辑资料")
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                        @Valid @RequestBody ProfileUpdateDTO dto) {
        return Result.ok(userService.updateProfile(userId, dto));
    }

    @Operation(summary = "#12 更换手机号")
    @PutMapping("/mobile")
    public Result<Void> changeMobile(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                     @Valid @RequestBody MobileChangeDTO dto) {
        userService.changeMobile(userId, dto);
        return Result.ok();
    }

    @Operation(summary = "#13 修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                                       @Valid @RequestBody PasswordChangeDTO dto) {
        userService.changePassword(userId, dto);
        return Result.ok();
    }

    @Operation(summary = "#14 我的信息概览")
    @GetMapping("/summary")
    public Result<UserSummaryVO> summary(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId) {
        return Result.ok(userService.getSummary(userId));
    }

}
