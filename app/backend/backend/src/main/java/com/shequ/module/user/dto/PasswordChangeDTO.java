package com.shequ.module.user.dto;

import com.shequ.common.constant.ValidationConst;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改密码（接口文档 #13）。oldPassword 与 smsCode 至少传其一。
 */
@Data
public class PasswordChangeDTO {

    /** 原密码（与 smsCode 二选一） */
    private String oldPassword;

    /** RESET 场景验证码（与 oldPassword 二选一） */
    @Pattern(regexp = ValidationConst.SMS_CODE, message = "验证码为 6 位数字")
    private String smsCode;

    @NotBlank(message = "请填写新密码")
    @Pattern(regexp = ValidationConst.PASSWORD, message = "密码需 8～20 位且同时包含字母与数字")
    private String newPassword;

    @NotBlank(message = "请再次输入新密码")
    private String confirmPassword;

}
