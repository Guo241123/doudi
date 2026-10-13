package com.shequ.module.user.dto;

import com.shequ.common.constant.ValidationConst;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 密码登录（接口文档 #5）。
 */
@Data
public class LoginPasswordDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = ValidationConst.MOBILE, message = "手机号格式不正确")
    private String mobile;

    @NotBlank(message = "密码不能为空")
    private String password;

}