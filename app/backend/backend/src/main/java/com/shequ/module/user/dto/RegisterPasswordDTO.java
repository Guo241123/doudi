package com.shequ.module.user.dto;

import com.shequ.common.constant.ValidationConst;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册②：设置密码（接口文档 #3）。
 */
@Data
public class RegisterPasswordDTO {

    @NotBlank(message = "注册凭证不能为空")
    private String registerToken;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = ValidationConst.PASSWORD, message = "密码需 8～20 位且同时包含字母与数字")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

}