package com.shequ.module.user.dto;

import com.shequ.common.constant.ValidationConst;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册①：校验手机号与验证码（接口文档 #2）。
 */
@Data
public class RegisterCheckMobileDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = ValidationConst.MOBILE, message = "手机号格式不正确")
    private String mobile;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = ValidationConst.SMS_CODE, message = "验证码为 6 位数字")
    private String smsCode;

}