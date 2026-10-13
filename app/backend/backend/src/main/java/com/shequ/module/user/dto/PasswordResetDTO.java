package com.shequ.module.user.dto;

import com.shequ.common.constant.ValidationConst;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 忘记密码-重置（接口文档 #7）。
 */
@Data
public class PasswordResetDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = ValidationConst.MOBILE, message = "手机号格式不正确")
    private String mobile;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = ValidationConst.SMS_CODE, message = "验证码为 6 位数字")
    private String smsCode;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = ValidationConst.PASSWORD, message = "密码需 8～20 位且同时包含字母与数字")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

}