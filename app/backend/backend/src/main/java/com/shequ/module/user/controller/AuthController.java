package com.shequ.module.user.controller;

import com.shequ.common.auth.AuthInterceptor;
import com.shequ.common.captcha.CaptchaService;
import com.shequ.common.captcha.CaptchaVO;
import com.shequ.common.result.Result;
import com.shequ.common.util.IpUtil;
import com.shequ.module.user.dto.LoginPasswordDTO;
import com.shequ.module.user.dto.LoginSmsDTO;
import com.shequ.module.user.dto.PasswordResetDTO;
import com.shequ.module.user.dto.RegisterCheckMobileDTO;
import com.shequ.module.user.dto.RegisterNicknameDTO;
import com.shequ.module.user.dto.RegisterPasswordDTO;
import com.shequ.module.user.dto.SmsSendDTO;
import com.shequ.module.user.service.AuthService;
import com.shequ.module.user.vo.LoginVO;
import com.shequ.module.user.vo.RegisterTokenVO;
import com.shequ.module.user.vo.SmsSendVO;
import com.shequ.module.user.vo.TokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口（接口文档 2.1 #0～#8）。
 */
@Tag(name = "认证与账号")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @Operation(summary = "#0 获取图形验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(captchaService.generate());
    }

    @Operation(summary = "#1 发送短信验证码")
    @PostMapping("/sms/send")
    public Result<SmsSendVO> sendSmsCode(@Valid @RequestBody SmsSendDTO dto, HttpServletRequest request) {
        return Result.ok(authService.sendSmsCode(dto, IpUtil.getClientIp(request)));
    }

    @Operation(summary = "#2 注册①校验手机号与验证码")
    @PostMapping("/register/check-mobile")
    public Result<RegisterTokenVO> registerCheckMobile(@Valid @RequestBody RegisterCheckMobileDTO dto) {
        return Result.ok(authService.registerCheckMobile(dto));
    }

    @Operation(summary = "#3 注册②设置密码")
    @PostMapping("/register/password")
    public Result<Void> registerSetPassword(@Valid @RequestBody RegisterPasswordDTO dto) {
        authService.registerSetPassword(dto);
        return Result.ok();
    }

    @Operation(summary = "#4 注册③设置昵称，完成注册")
    @PostMapping("/register/nickname")
    public Result<LoginVO> registerSetNickname(@Valid @RequestBody RegisterNicknameDTO dto) {
        return Result.ok(authService.registerSetNickname(dto));
    }

    @Operation(summary = "#5 密码登录")
    @PostMapping("/login/password")
    public Result<LoginVO> loginByPassword(@Valid @RequestBody LoginPasswordDTO dto, HttpServletRequest request) {
        return Result.ok(authService.loginByPassword(dto, IpUtil.getClientIp(request)));
    }

    @Operation(summary = "#6 验证码登录")
    @PostMapping("/login/sms")
    public Result<LoginVO> loginBySms(@Valid @RequestBody LoginSmsDTO dto) {
        return Result.ok(authService.loginBySms(dto));
    }

    @Operation(summary = "#7 忘记密码-重置")
    @PostMapping("/password/reset")
    public Result<TokenVO> resetPassword(@Valid @RequestBody PasswordResetDTO dto) {
        return Result.ok(authService.resetPassword(dto));
    }

    @Operation(summary = "#8 退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) long userId,
                              @RequestAttribute(AuthInterceptor.ATTR_JTI) String jti) {
        authService.logout(userId, jti);
        return Result.ok();
    }

}