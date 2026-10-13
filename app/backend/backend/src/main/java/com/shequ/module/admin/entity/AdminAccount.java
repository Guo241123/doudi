package com.shequ.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员表实体（接口文档 #47）。
 * ID 与 t_user 独立编号空间，token 白名单键 admin:tokens:{id} 互不冲突。
 */
@Data
@TableName("t_admin")
public class AdminAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名 */
    private String username;

    /** 密码 BCrypt 哈希（加盐），禁止明文 */
    private String passwordHash;

    /** 显示昵称（公告作者 / 客服身份） */
    private String nickname;

    /** 0 启用 / 1 停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 软删除：0 正常 / 1 已删除 */
    private Integer isDeleted;

}
