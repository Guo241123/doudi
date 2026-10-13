package com.shequ.module.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 管理员登录返回体（接口文档 #47：token 2 小时）。
 */
@Data
@AllArgsConstructor
public class AdminLoginVO {

    private Long adminId;

    private String nickname;

    /** 访问 token，2 小时有效 */
    private String token;

}
