package com.shequ.module.user.dto;

import com.shequ.common.constant.ValidationConst;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送短信验证码（接口文档 #1）。
 */
@Data
public class SmsSendDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = ValidationConst.MOBILE, message = "手机号格式不正确")
    private String mobile;

    @NotBlank(message = "短信场景不能为空")
    @Pattern(regexp = ValidationConst.SMS_SCENE, message = "短信场景不合法")
    private String scene;

    @NotBlank(message = "图形验证码会话不能为空")
    private String captchaId;

    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;

}