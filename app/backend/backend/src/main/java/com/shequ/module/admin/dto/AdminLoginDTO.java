package com.shequ.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员登录请求体（接口文档 #47）。
 */
@Data
public class AdminLoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 32, message = "用户名不能超过 32 字")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码不能超过 64 字")
    private String password;

}
