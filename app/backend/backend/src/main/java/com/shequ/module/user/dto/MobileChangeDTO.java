package com.shequ.module.user.dto;

import com.shequ.common.constant.ValidationConst;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 更换手机号（接口文档 #12）。验证码发到新手机号（CHANGE_MOBILE 场景）。
 */
@Data
public class MobileChangeDTO {

    @NotBlank(message = "请填写新手机号")
    @Pattern(regexp = ValidationConst.MOBILE, message = "手机号格式不正确")
    private String newMobile;

    @NotBlank(message = "请填写短信验证码")
    @Pattern(regexp = ValidationConst.SMS_CODE, message = "验证码为 6 位数字")
    private String smsCode;

}
