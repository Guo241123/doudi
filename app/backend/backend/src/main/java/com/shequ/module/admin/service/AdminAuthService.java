package com.shequ.module.admin.service;

import com.shequ.module.admin.dto.AdminLoginDTO;
import com.shequ.module.admin.vo.AdminLoginVO;

/**
 * 管理员认证服务（接口文档 #47）。
 */
public interface AdminAuthService {

    /**
     * 管理员登录：连续错 5 次锁 15 分钟（与 App 登录同规则）；签发 2 小时 ADMIN token。
     */
    AdminLoginVO login(AdminLoginDTO dto);

    /**
     * 管理员显示昵称（消息模块会话列表展示客服身份用，不存在返回 null）。
     */
    String nicknameOf(Long adminId);

}
